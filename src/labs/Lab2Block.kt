package labs

import commWorld
import recipientRankOrNull
import senderRankOrNull

private const val TAG = 0
private const val centerRank = 0

fun main(args: Array<String>) = commWorld(args) { commWorld ->
    val message = IntArray(1)
    val rank = commWorld.rank
    val size = commWorld.size

    if (rank != centerRank) {
        senderRankOrNull(rank)?.let { senderRank ->
            val messageReceive = commWorld.receive(message.size, source = senderRank, tag = TAG)
            message[0] += messageReceive[0] + rank
        }
    }

    commWorld.send(
        message,
        destination = recipientRankOrNull(rank, size) ?: centerRank,
        tag = TAG
    )

    if (rank == centerRank) {
        val senderRank = senderRankOrNull(rank = size) ?: throw error("Size too small: $size")
        val messageReceive = commWorld.receive(message.size, source = senderRank, tag = TAG)
        println("My rank: $rank I got sum: ${messageReceive[centerRank]} from: $senderRank")
    }
}
