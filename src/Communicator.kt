@file:Suppress("NOTHING_TO_INLINE")

import mpi.Intracomm
import mpi.MPI
import mpi.Request

/** Запуск [MPI] и гарантированная отчистка ресурсов после завершения. */
inline fun commWorld(args: Array<String>, block: (communicator: Communicator) -> Unit) {
    MPI.Init(args)
    try {
        block(Communicator(MPI.COMM_WORLD))
    } finally {
        MPI.Finalize()
    }
}

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
        message: IntArray,
        destination: Int,
        tag: Int,
        size: Int = message.size,
        offset: Int = 0
    ): Unit =
        intracomm.Send(message, offset, size, MPI.INT, destination, tag)

    /**
     * Получение сообщения.
     * @param messSize размер ожидаемого сообщения.
     * @param source ранк, откуда доставляется сообщение.
     * @return сообщение в виде массива чисел.
     */
    inline fun receive(
        messSize: Int,
        source: Int,
        tag: Int,
        offset: Int = 0
    ): IntArray =
        IntArray(messSize).also { intracomm.Recv(it, offset, messSize, MPI.INT, source, tag) }

    /** Асинхронный вариант [send]. */
    inline fun asyncSend(
        message: IntArray,
        destination: Int,
        tag: Int,
        size: Int = message.size,
        offset: Int = 0
    ): Request =
        intracomm.Isend(message, offset, size, MPI.INT, destination, tag)

    /** Асинхронный вариант [receive]. */
    inline fun asyncReceive(
        messSize: Int,
        source: Int,
        tag: Int,
        offset: Int = 0
    ): Pair<IntArray, Request> {
        val arr = IntArray(messSize)
        val request = intracomm.Irecv(arr, offset, messSize, MPI.INT, source, tag)
        return arr to request
    }
}
