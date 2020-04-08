package data

/** Информация, упрощающая работу при коммуникации между процессами. */
class CommunicationInfo(communicator: Communicator, messageSize: Int) {
    /** Размер каждого каждого подсообщения, которое можно отослать другому ранку. */
    val subMessageSize: Int

    /** Ранки, которые будут получать информацию. Не входят простаивающие ранки, которым нечем заняться. */
    val receivingRanks: IntRange

    init {
        assert(messageSize > 0)
        subMessageSize = (messageSize / communicator.numberOfRanks.toDouble())
            .let { if (it.decimalPart() > 0) it + 1 else it }
            .toInt()
        receivingRanks = 0..(messageSize / subMessageSize)
    }

    private fun Double.decimalPart(): Double =
        this - this.toInt()
}
