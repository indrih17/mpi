package labs.lab5

import data.Communicator

/** Информация, упрощающая работу при коммуникации между процессами. */
class CommunicationInfo(communicator: Communicator, messageSize: Int) {
    /** Размер каждого каждого подсообщения, которое можно отослать другому ранку. */
    val subMessageSize: Int

    /** Диапазон дополнительных сообщений. */
    val subMessageRange: IntRange

    /** Ранки, которые будут получать информацию. Не входят простаивающие ранки. */
    val receivingRanks: IntRange

    init {
        assert(messageSize > 0)

        subMessageSize = (messageSize / (communicator.size - 1).toDouble())
            .let { if (it.decimalPart() > 0) it + 1 else it }
            .toInt()
        subMessageRange = 1..subMessageSize
        receivingRanks = 1..(messageSize / subMessageSize)
    }

    private fun Double.decimalPart(): Double =
        this - this.toInt()
}
