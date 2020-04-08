package data

import mpi.Intracomm
import mpi.MPI
import mpi.Request
import mpi.Status
import mpi.Op
import java.nio.ByteBuffer

/** Запуск [MPI] и гарантированная отчистка ресурсов после завершения. */
inline fun commWorld(args: Array<String>, block: (communicator: Communicator) -> Unit) {
    MPI.Init(args)
    block(Communicator(MPI.COMM_WORLD))
    MPI.Finalize()
}

/** @return Ранк кому доставить сообщение. Может не быть, если отправитель последний (size нечётный). */
fun recipientRankOrNull(rank: Int, size: Int): Int? =
    (rank + 1).takeIf { it < size }

/** @return Ранк от кого получить сообщение. Может не быть, если получатель первый. */
fun senderRankOrNull(rank: Int): Int? =
    (rank - 1).takeIf { it >= 0 }

const val mainTag = 0
const val centerRank = 0

/**
 * Обёртка над коммуникатором для использования в Kotlin style.
 * Коммуникатор - множество процессов, образующих логическую область для выполнения
 * коллективных операций (обменов информацией и др.).
 *
 * Блокирующие вызовы отправки и получения приостанавливают исполнение программы до момента, когда данные
 * будут отправлены (скопированы из буфера отправки), но они не обязаны быть получены получающей задачей.
 * Содержимое буфера отправки теперь может быть безопасно модифицировано без воздействия на отправленное сообщение.
 * Завершение блокирующего обмена подразумевает, что данные в буфере получения правильные.
 *
 * Неблокирующие вызовы возвращаются немедленно после инициации коммуникации. Программист не знает, скопированы ли
 * отправляемые данные из буфера отправки, или являются ли передаваемые данные прибывшими к получателю.
 * Таким образом, перед очередным использованием буфера сообщения программист должен проверить его статус.
 */
inline class Communicator(val intracomm: Intracomm) {
    val rank: Int get() = intracomm.Rank()
    val numberOfRanks: Int get() = intracomm.Size()

    /**
     * Блокирующая отправка сообщения.
     * @param message сообщение в виде массива чисел.
     * @param destination ранк, куда доставляется сообщение.
     */
    fun send(
        message: Message,
        destination: Int,
        tag: Int = mainTag
    ) {
        intracomm.Send(message, 0, message.size, MPI.INT, destination, tag)
    }

    /**
     * Блокирующее получение сообщения.
     * @param size размер ожидаемого сообщения.
     * @param source ранк, откуда доставляется сообщение.
     * @return сообщение в виде массива чисел.
     *
     * Может принимать сообщения, отправленные в любом режиме.
     *
     * Значение параметра [size] может оказаться больше, чем количество элементов в принятом сообщении.
     * В этом случае после выполнения приёма в буфере изменится значение только тех элементов,
     * которые соответствуют элементам фактически принятого сообщения.
     */
    fun receive(
        size: Int,
        source: Int,
        tag: Int = mainTag
    ): Message =
        Message(size).also { intracomm.Recv(it, 0, size, MPI.INT, source, tag) }

    /** Асинхронный вариант [send]. */
    fun asyncSend(
        message: Message,
        destination: Int,
        tag: Int = mainTag
    ): Request =
        intracomm.Isend(message, 0, message.size, MPI.INT, destination, tag)

    /** Асинхронный вариант [receive]. */
    fun asyncReceive(
        size: Int,
        source: Int,
        tag: Int = mainTag
    ): Pair<Message, Request> {
        val message = Message(size)
        val request = intracomm.Irecv(message, 0, size, MPI.INT, source, tag)
        return message to request
    }

    /** Позволяет проверить входные сообщения без их реального приема. */
    fun probe(source: Int, tag: Int = mainTag): Status =
        intracomm.Probe(source, tag)

    /**
     * Является чем-то средним между [send] и [asyncSend]. Оригинальное сообщение копируется в создаваемый нами буфер,
     * а далее происходит асинхронная отправка. Как только сообщение будет скопировано, функция тут же вернёт нам управление.
     */
    fun bufferSend(
        message: Message,
        destination: Int,
        tag: Int = mainTag
    ) {
        val size = message.size
        val minOverhead = 15
        val overhead = (MPI.BSEND_OVERHEAD.takeIf { it != 0 } ?: size * 10).takeIf { it >= minOverhead } ?: minOverhead
        val byteBuffer = ByteBuffer.allocate(size + overhead)
        MPI.Buffer_attach(byteBuffer)
        intracomm.Bsend(message, 0, size, MPI.INT, destination, tag)
        MPI.Buffer_detach() // блокирует работу процесса до тех пор, пока все сообщения, находящиеся в буфере, не будут обработаны
    }

    /**
     * С обычным [send], реализация вернется в приложение, когда буфер станет доступен для повторного использования.
     * Это возможно перед тем, как процесс-получатель фактически разместил сообщение о приеме.
     * Например, это может быть, когда небольшое сообщение было скопировано во внутренний буфер и буфер приложения больше не требуется.
     * Однако, для больших сообщений, которые не могут быть буферизованы сразу целиком, вызов не может быть возвращен,
     * пока достаточная часть сообщения не была отправлена на удаленный процесс.
     *
     * А в случае с [syncSend], завершение передачи происходит только после того, как прием сообщения закончен другим процессом.
     * Адресат посылает источнику «квитанцию» - уведомление о завершении приема.
     * После получения этого уведомления обмен считается завершенным и источник "знает", что его сообщение получено.
     */
    fun syncSend(
        message: Message,
        destination: Int,
        tag: Int = mainTag
    ) {
        intracomm.Ssend(message, 0, message.size, MPI.INT, destination, tag)
    }

    /**
     * Отправка способом по готовности требует, чтобы прибыло уведомление "готов к получению".
     * Если сообщение не прибыло, то отправка способом по готовности выдаст ошибку.
     * Способ по готовности имеет целью минимизировать системное ожидание и синхронизационное ожидание, вызванное задачей отправления.
     *
     * Передача «по готовности» должна начинаться, если уже зарегистрирован соответствующий прием.
     * При несоблюдении этого условия результат выполнения операции не определен.
     *
     * 1) Завершение передачи не зависит от того, вызвана ли другим процессом подпрограмма приема данного сообщения или нет,
     * оно означает только, что буфер передачи можно использовать вновь.
     * 2) Сообщение просто выбрасывается в коммуникационную сеть в надежде, что адресат его получит. Эта надежда может и не сбыться.
     * 3) Обмен «по готовности» может увеличить производительность программы, поскольку здесь не используются
     * этапы установки межпроцессных связей, а также буферизация. Все это - операции, требующие времени.
     * 4) Таким образом, обмен «по готовности» быстр, но потенциально опасен: он усложняет отладку,
     * поэтому его рекомендуется использовать только в том случае, когда правильная работа программы гарантируется
     * её логической структурой, а выигрыша в быстродействии надо добиться любой ценой.
     */
    fun readySend(
        message: Message,
        destination: Int,
        tag: Int = mainTag
    ) {
        intracomm.Rsend(message, 0, message.size, MPI.INT, destination, tag)
    }

    /**
     * Рассылка [message] всем процессам.
     * По сути, [root] ранк просто раздаёт всем остальным ранкам копию [Message].
     * @param root ранг главного процесса, выполняющего широковещательную рассылку.
     */
    fun broadcast(message: Message, root: Int): Message =
        message.copyOf().also { new ->
            intracomm.Bcast(new, 0, new.size, MPI.INT, root)
        }

    /**
     * Если поток не явлется [root], то отправляет [message] в [root].
     * Если поток является [root], получает от всех потоков подмесседжы, мержит их в [Message]
     * и применяет операцию [operation], в результате чего получается msg с 1 значением.
     */
    fun reduce(
        message: Message,
        root: Int,
        operation: Operation
    ): Message {
        val size: Int = message.size
        val receive = Message(size)
        intracomm.Reduce(message, 0, receive, 0, size, MPI.INT, operation.op, root)
        return receive
    }

    /**
     * Дробит [message] на равные подсообщения и отправляет каждому по сообщению.
     * @throws IllegalStateException если невозможно каждому ранку дать по равному сообщению.
     */
    fun scatter(message: Message, root: Int): Message {
        val subMessageSize = message.size / numberOfRanks
        if (subMessageSize * numberOfRanks != message.size)
            error("Неправильное соотношение размера и количества потоков.")
        val receive = Message(subMessageSize)
        intracomm.Scatter(message, 0, subMessageSize, MPI.INT, receive, 0, subMessageSize, MPI.INT, root)
        return receive
    }

    /**
     * Если поток не явлется [root], то отправляет [message] в [root].
     * Если поток является [root], получает от всех потоков подмесседжы и мержит их в [Message].
     */
    fun gather(message: Message, root: Int): Message =
        Message(numberOfRanks * message.size).also {
            intracomm.Gather(message, 0, message.size, MPI.INT, it, 0, message.size, MPI.INT, root)
        }
}

enum class Operation(val op: Op) {
    Sum(MPI.SUM)
}

