package data

typealias Message = IntArray

fun messageOf(vararg elements: Int) = intArrayOf(*elements)

fun List<Int>.toMessage() = toIntArray()

infix fun Message.multiple(other: Message): Message {
    assert(size == other.size)
    return mapIndexed { index, number -> number * other[index] }.toMessage()
}

fun List<Message>.merge(): Message =
    fold(messageOf()) { acc, message -> acc + message }

fun List<Message>.stringify(): String =
    mapIndexed { index, msg -> "Matrix $index: ${msg.contentToString()}" }
        .joinToString(separator = "\n")

fun List<Message>.getColumn(index: Int): Message =
    map { it[index] }.toMessage()

/**
 * Разбить вектор на подсписки размером [step] и трансформацией каждого списка в [R].
 * Если последний список меньше размером, то выдаётся таким.
 *
 * Пример:
 * messageOf(1, 2, 3, 4, 5)
 *     .split(step = 3) { it }
 *     .forEach { println(it) }
 *
 * Вывод результата:
 * [1, 2, 3]
 * [4, 5]
 *
 * @throws IllegalStateException если [step] <= 0 или [step] >= size.
 */
inline fun <R> Message.split(step: Int, transform: (Message) -> R): List<R> =
    splitWithIterationNumber(step) { _, msg -> transform(msg) }

/** То же самое, что и [split], только [transform] ещё содержит номер итерации. */
inline fun <R> Message.splitWithIterationNumber(
    step: Int,
    transform: (iteration: Int, msg: Message) -> R
): List<R> {
    var iteration = 1
    return splitIndexed(step) { _, _, msg -> transform(iteration++, msg) }
}

/** То же самое, что и [split], только [transform] ещё содержит индексы. */
inline fun <R> Message.splitIndexed(
    step: Int,
    transform: (from: Int, to: Int, msg: Message) -> R
): List<R> {
    assert(step > 0) { "Шаг должен быть больше 0" }
    val result = ArrayList<R>(size / step)
    var index = 0
    while (index < size) {
        val fromIndex = index
        val toIndexExclusive = (index + step).takeIf { it <= size } ?: size
        result.add(transform(fromIndex, toIndexExclusive, copyOfRange(fromIndex, toIndexExclusive)))
        index += step
    }
    return result
}
