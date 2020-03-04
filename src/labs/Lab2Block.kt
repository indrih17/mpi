package labs

import commWorld
import recipientRankOrNull
import senderRankOrNull

private const val TAG = 0
private const val centerRank = 0

/**
 * Блокирующий вариант задачи.
 * Каждый процессор помещает свой ранг в целочисленную переменную buf.
 * Каждый процессор пересылает переменную buf соседу справа (по часовой стрелке по кольцу).
 * Каждый процессор суммирует принимаемое значение в переменную S, а затем передаёт рассчитанное значение соседу справа.
 * Пересылки по кольцу прекращаются, когда нулевой процессор просуммирует ранги всех процессоров.
 */
fun main(args: Array<String>) = commWorld(args) { communicator ->
    val message = intArrayOf(0)
    val rank = communicator.rank
    val size = communicator.size

    if (rank != centerRank) {
        senderRankOrNull(rank)?.let { senderRank ->
            val messageReceive = communicator.receive(message.size, source = senderRank, tag = TAG)
            message[0] += messageReceive[0] + rank
        }
    }

    communicator.send(message, destination = recipientRankOrNull(rank, size) ?: centerRank, tag = TAG)

    if (rank == centerRank) {
        val senderRank = senderRankOrNull(rank = size) ?: throw error("Size too small: $size")
        val messageReceive = communicator.receive(message.size, source = senderRank, tag = TAG)
        println("My rank: $rank I got sum: ${messageReceive[centerRank]} from: $senderRank")
    }
}
