package data

typealias Message = IntArray

fun messageOf(vararg elements: Int) = intArrayOf(*elements)

fun List<Int>.toMessage() = toIntArray()

operator fun Message.times(other: Message): Message {
    assert(size == other.size)
    return mapIndexed { index, number -> number * other[index] }.toMessage()
}

fun List<Message>.merge(): Message =
    fold(messageOf()) { acc, message -> acc + message }

fun List<Message>.stringify(): String =
    mapIndexed { index, msg -> "Matrix $index: ${msg.contentToString()}" }
        .joinToString(separator = "\n")
