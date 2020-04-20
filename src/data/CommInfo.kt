package data

/** Информация, упрощающая работу при коммуникации между процессами. */
class CommInfo(private val communicator: Communicator, private val messageSize: Int) {
    /** Размер каждого каждого подсообщения, которое можно отослать другому ранку. */
    private val subMsgSizeRaw: Int = messageSize divideToUpper communicator.numberOfRanks

    /** Ранки, которые будут получать информацию. Не входят простаивающие ранки, которым нечем заняться. */
    val receivingRanks: IntRange = 0..(messageSize / subMsgSizeRaw)

    /** Умная версия [subMsgSizeRaw], которая отрезает лишнее. */
    val subMessageSize: Int
        get() {
            val ranks = receivingRanks.toList().size
            return if (communicator.rank == receivingRanks.last)
                subMsgSizeRaw - (subMsgSizeRaw * ranks - messageSize)
            else
                subMsgSizeRaw
        }

    /** Промежуток индексов, который необходимо обработать текущему ранку. */
    val rangeForRank: IntRange

    init {
        assert(messageSize > 0)
        val from = communicator.rank * subMsgSizeRaw
        val to = from + subMessageSize
        rangeForRank = from until to
    }

    private infix fun Int.divideToUpper(other: Int): Int =
        (this / other.toDouble())
            .let { if (it.decimalPart() > 0) it + 1 else it }
            .toInt()

    private fun Double.decimalPart(): Double =
        this - this.toInt()
}
