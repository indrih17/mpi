package labs

import commWorld
import recipientRankOrNull
import senderRankOrNull
import stringify

private const val TAG = 0

fun main(args: Array<String>) = commWorld(args) { commWorld ->
    val message = intArrayOf(42, 17)
    val rank = commWorld.rank
    val size = commWorld.size

    if (rank.isSendRank) {
        recipientRankOrNull(rank, size)?.let { recipientRank ->
            commWorld.send(message, destination = recipientRank, tag = TAG)
        }
    } else {
        senderRankOrNull(rank)?.let { senderRank ->
            val received = commWorld.receive(message.size, source = senderRank, tag = TAG)
            println("I'm rank: $rank! received: ${received.stringify()} from rank: $senderRank!")
        }
    }
}

/** 0 - отправитель, 1 - получатель, 2 - отправитель, .. */
private val Int.isSendRank: Boolean inline get() = this % 2 == 0
