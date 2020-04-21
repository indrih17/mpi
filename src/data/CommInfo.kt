package data

import copyOfRange

/** Информация, упрощающая работу при коммуникации между процессами. */
class CommInfo(
    communicator: Communicator,
    private val messageSize: Int,
    centralRankCollectsData: Boolean
) {
    private val centerRankShift = if (communicator.numberOfRanks > 1 && centralRankCollectsData) 1 else 0

    /** Размер каждого каждого подсообщения, которое можно отослать другому ранку. */
    private val subMsgSizeRaw: Int =
        if (messageSize >= communicator.numberOfRanks)
            messageSize / (communicator.numberOfRanks - centerRankShift)
        else
            1

    /** Ранки, которые будут получать информацию. Не входят простаивающие ранки, которым нечем заняться. */
    val receivingRanks: IntRange = centerRankShift..(messageSize / subMsgSizeRaw)

    /** Умная версия [subMsgSizeRaw], которая отрезает лишнее. */
    private fun subMessageSize(rank: Rank): Int {
        val ranks = receivingRanks.toList().size
        return if (rank == receivingRanks.last)
            subMsgSizeRaw - (subMsgSizeRaw * ranks - messageSize)
        else
            subMsgSizeRaw
    }

    /** Промежуток индексов, который необходимо обработать текущему ранку. */
    fun rangeForRank(rank: Rank): IntRange {
        val from = (rank - centerRankShift) * subMsgSizeRaw
        val to = from + subMessageSize(rank)
        return from until to
    }

    init {
        check(messageSize > 0)
    }
}

fun CommInfo.split(message: Message): Map<Rank, Message> =
    receivingRanks
        .map { rank ->
            val range = rangeForRank(rank)
            val msg = message.copyOfRange(range.first, range.last + 1)
            rank to msg
        }
        .toMap()

fun CommInfo.split(messages: Iterable<Message>): Map<Rank, List<Message>> =
    receivingRanks
        .map { rank ->
            val range = rangeForRank(rank)
            val msgList = messages.copyOfRange(range.first, range.last + 1)
            rank to msgList
        }
        .toMap()
