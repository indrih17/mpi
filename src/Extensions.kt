import data.Message
import mpi.MPI
import mpi.Request
import mpi.Status

/** Маппит список с учётом предыдущего элемента. */
inline fun <T : Any, R> List<T>.mapWithPrevious(block: (prev: T, curr: T) -> R): List<R> {
    var prev: T = firstOrNull() ?: return emptyList()
    return minus(prev).map { curr -> block(prev, curr).also { prev = curr } }
}

/** Длина сообщения. */
fun Status.getLength(): Int = Get_count(MPI.INT)

fun List<Request>.awaitAll(): List<Status> = map { it.Wait() }

fun Pair<Message, Request>.await(): Message {
    second.Wait()
    return first
}

fun List<Pair<Message, Request>>.awaitAllMessages(): List<Message> =
    map(Pair<Message, Request>::await)

inline fun <reified T> it(t: T): T = t
