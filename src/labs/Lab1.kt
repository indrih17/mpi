package labs

import commWorld
import recipientRankOrNull
import senderRankOrNull
import stringify

private const val TAG = 0

/**
 * При запуске четного числа процессов, те из них, которые имеют чётный ранг,
 * отправляют сообщение следующим по величине ранга процессам.
 */
fun main(args: Array<String>) = commWorld(args) { communicator ->
    val message = intArrayOf(42, 17)
    val rank = communicator.rank
    val size = communicator.size

    if (rank.isSendRank) {
        recipientRankOrNull(rank, size)?.let { recipientRank ->
            communicator.send(message, destination = recipientRank, tag = TAG)
        }
    } else {
        senderRankOrNull(rank)?.let { senderRank ->
            val received = communicator.receive(message.size, source = senderRank, tag = TAG)
            println("I'm rank: $rank! received: ${received.stringify()} from rank: $senderRank!")
        }
    }
}

/** 0 - отправитель, 1 - получатель, 2 - отправитель, .. */
private val Int.isSendRank: Boolean inline get() = this % 2 == 0
