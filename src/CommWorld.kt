@file:Suppress("NOTHING_TO_INLINE")

import mpi.Intracomm
import mpi.MPI
import mpi.Request

/** Запуск [MPI] и гарантированная отчистка ресурсов после завершения. */
inline fun commWorld(args: Array<String>, block: (commWorld: CommWorld) -> Unit) {
    MPI.Init(args)
    try {
        block(CommWorld(MPI.COMM_WORLD))
    } finally {
        MPI.Finalize()
    }
}

inline class CommWorld(val intracomm: Intracomm) {
    val rank: Int get() = intracomm.Rank()
    val size: Int get() = intracomm.Size()

    /**
     * Отправка сообщения.
     * @param message сообщение в виде массива чисел.
     * @param destination ранк, куда доставляется сообщение.
     */
    inline fun send(message: IntArray, destination: Int, tag: Int): Unit =
        intracomm.Send(message, 0, message.size, MPI.INT, destination, tag)

    /**
     * Получение сообщения.
     * @param messSize размер ожидаемого сообщения.
     * @param source ранк, откуда доставляется сообщение.
     * @return сообщение в виде массива чисел.
     */
    inline fun receive(messSize: Int, source: Int, tag: Int): IntArray =
        IntArray(messSize).also { intracomm.Recv(it, 0, messSize, MPI.INT, source, tag) }

    /** Асинхронный вариант [send]. */
    inline fun asyncSend(message: IntArray, destination: Int, tag: Int): Request =
        intracomm.Isend(message, 0, message.size, MPI.INT, destination, tag)

    /** Асинхронный вариант [receive]. */
    inline fun asyncReceive(messSize: Int, source: Int, tag: Int): Pair<IntArray, Request> {
        val arr = IntArray(messSize)
        val request = intracomm.Irecv(arr, 0, messSize, MPI.INT, source, tag)
        return arr to request
    }
}
