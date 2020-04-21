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

fun <T> Map<T, T>.flatten(): List<T> {
    val result = ArrayList<T>()
    for ((k, v) in this) {
        result.add(k)
        result.add(v)
    }
    return result
}

inline fun <reified T> Map<Int, T>.toIndexedArray(): Array<T> =
    Array(size) { this[it] ?: error("No found $it") }

/**
 * Разбить вектор на подсписки размером [step] и трансформацией каждого списка в [R].
 * Если последний список меньше размером, то выдаётся таким.
 *
 * Пример:
 * messageOf(1, 2, 3, 4, 5)
 *     .asIterable()
 *     .split(step = 3) { it }
 *     .forEach { println(it) }
 *
 * Вывод результата:
 * [1, 2, 3]
 * [4, 5]
 *
 * @throws IllegalStateException если [step] <= 0 или [step] >= size.
 */
inline fun <T, R> Iterable<T>.split(step: Int, transform: (List<T>) -> R): List<R> {
    assert(step > 0) { "Шаг должен быть больше 0" }
    val size = count()
    val result = ArrayList<R>(size / step)
    var index = 0
    while (index < size) {
        val fromIndex = index
        val toIndexExclusive = (index + step).takeIf { it <= size } ?: size
        result.add(transform(copyOfRange(fromIndex, toIndexExclusive)))
        index += step
    }
    return result
}

fun <T> Iterable<T>.copyOfRange(from: Int, to: Int): List<T> =
    drop(from).take(to - from)

fun <T> Iterable<T>.isEachItemUnique(): Boolean =
    toSet().toList() == this.toList()
