package labs.lab2

import data.commWorld
import data.messageOf
import data.senderRankOrNull
import data.recipientRankOrNull
import data.centerRank

/**
 * Блокирующий вариант задачи.
 * Каждый процессор помещает свой ранг в целочисленную переменную buf.
 * Каждый процессор пересылает переменную buf соседу справа (по часовой стрелке по кольцу).
 * Каждый процессор суммирует принимаемое значение в переменную S, а затем передаёт рассчитанное значение соседу справа.
 * Пересылки по кольцу прекращаются, когда нулевой процессор просуммирует ранги всех процессоров.
 */
fun main(args: Array<String>) = commWorld(args) { communicator ->
    val message = messageOf(0)
    val rank = communicator.rank
    val size = communicator.numberOfRanks

    if (rank != centerRank) {
        senderRankOrNull(rank)?.let { senderRank ->
            val messageReceive = communicator.receive(source = senderRank)
            message[0] += messageReceive[0] + rank
        }
    }

    communicator.send(message, destination = recipientRankOrNull(rank, size) ?: centerRank)

    if (rank == centerRank) {
        val senderRank = senderRankOrNull(rank = size) ?: throw error("Size too small: $size")
        val messageReceive = communicator.receive(source = senderRank)
        println("My rank: $rank I got sum: ${messageReceive[centerRank]} from: $senderRank")
    }
}
