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
            val (messageReceive, request) = commWorld.asyncReceive(message.size, source = senderRank, tag = TAG)
            println("I'm rank: $rank Without wait I received: ${if (messageReceive[0] != 0) "Something" else "Nothing"}")
            request.Wait()
            println("I'm rank: $rank With wait I received: ${if (messageReceive[0] != 0) "Something" else "Nothing"}")
            message[0] += messageReceive[0] + rank
        }
    }

    commWorld.asyncSend(
        message,
        destination = recipientRankOrNull(rank, size) ?: centerRank,
        tag = TAG
    )

    if (rank == centerRank) {
        val senderRank = senderRankOrNull(rank = size) ?: throw error("Size too small: $size")
        val (messageReceive, request) = commWorld.asyncReceive(message.size, source = senderRank, tag = TAG)
        request.Wait()
        println("My rank: $rank I got sum: ${messageReceive[centerRank]} from: $senderRank")
    }
}
