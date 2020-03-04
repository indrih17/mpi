package labs

import commWorld
import recipientRankOrNull
import senderRankOrNull

private const val TAG = 0
private const val centerRank = 0

/**
 * Асинхронный вариант задачи.
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
            val (messageReceive, request) = communicator.asyncReceive(message.size, source = senderRank, tag = TAG)
            println("I'm rank: $rank Without wait I received: ${if (messageReceive[0] != 0) "Something" else "Nothing"}")
            request.Wait()
            println("I'm rank: $rank With wait I received: ${if (messageReceive[0] != 0) "Something" else "Nothing"}")
            message[0] += messageReceive[0] + rank
        }
    }

    communicator.asyncSend(message, destination = recipientRankOrNull(rank, size) ?: centerRank, tag = TAG)

    if (rank == centerRank) {
        val senderRank = senderRankOrNull(rank = size) ?: throw error("Size too small: $size")
        val (messageReceive, request) = communicator.asyncReceive(message.size, source = senderRank, tag = TAG)
        request.Wait()
        println("My rank: $rank I got sum: ${messageReceive[centerRank]} from: $senderRank")
    }
}
