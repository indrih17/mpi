@file:Suppress("NOTHING_TO_INLINE")

package data

import mpi.Intracomm
import mpi.MPI
import mpi.Request
import mpi.Status

/** Запуск [MPI] и гарантированная отчистка ресурсов после завершения. */
inline fun commWorld(args: Array<String>, block: (communicator: Communicator) -> Unit) {
    MPI.Init(args)
    try {
        block(Communicator(MPI.COMM_WORLD))
    } finally {
        MPI.Finalize()
    }
}

/** @return Ранк кому доставить сообщение. Может не быть, если отправитель последний (size нечётный). */
inline fun recipientRankOrNull(rank: Int, size: Int): Int? =
    (rank + 1).takeIf { it < size }

/** @return Ранк от кого получить сообщение. Может не быть, если получатель первый. */
inline fun senderRankOrNull(rank: Int): Int? =
    (rank - 1).takeIf { it >= 0 }

const val mainTag = 0
const val centerRank = 0

/** Обёртка над коммуникатором для использования в Kotlin style. */
inline class Communicator(val intracomm: Intracomm) {
    val rank: Int get() = intracomm.Rank()
    val size: Int get() = intracomm.Size()

    /**
     * Отправка сообщения.
     * @param message сообщение в виде массива чисел.
     * @param destination ранк, куда доставляется сообщение.
     */
    inline fun send(
        message: Message,
        destination: Int,
        tag: Int = mainTag,
        size: Int = message.size,
        offset: Int = 0
    ): Unit =
        intracomm.Send(message.intArray, offset, size, MPI.INT, destination, tag)

    /**
     * Получение сообщения.
     * @param size размер ожидаемого сообщения.
     * @param source ранк, откуда доставляется сообщение.
     * @return сообщение в виде массива чисел.
     */
    inline fun receive(
        size: Int,
        source: Int,
        tag: Int = mainTag
    ): Message =
        Message(size).also { intracomm.Recv(it.intArray, 0, size, MPI.INT, source, tag) }

    /** Асинхронный вариант [send]. */
    inline fun asyncSend(
        message: Message,
        destination: Int,
        tag: Int = mainTag,
        size: Int = message.size,
        offset: Int = 0
    ): Request =
        intracomm.Isend(message.intArray, offset, size, MPI.INT, destination, tag)

    /** Асинхронный вариант [receive]. */
    inline fun asyncReceive(
        size: Int,
        source: Int,
        tag: Int = mainTag
    ): Pair<Message, Request> {
        val message = Message(size)
        val request = intracomm.Irecv(message.intArray, 0, size, MPI.INT, source, tag)
        return message to request
    }

    /** Позволяет проверить входные сообщения без их реального приема. */
    inline fun probe(source: Int, tag: Int = mainTag): Status =
        intracomm.Probe(source, tag)
}
