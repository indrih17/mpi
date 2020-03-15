import mpi.MPI
import mpi.Status

/** Маппит список с учётом предыдущего элемента. */
inline fun <T : Any, R> List<T>.mapWithPrevious(block: (prev: T, curr: T) -> R): List<R> {
    var prev: T = firstOrNull() ?: return emptyList()
    return minus(prev).map { curr -> block(prev, curr).also { prev = curr } }
}

/** Длина сообщения. */
fun Status.getLength(): Int = Get_count(MPI.INT)
