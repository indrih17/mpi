package labs

import data.awaitAll
import data.commWorld
import data.Message
import data.centerRank
import data.merge
import mapWithPrevious
import kotlin.math.roundToInt
import kotlin.random.Random

/** Количество элементов в одном сообщении. */
private const val everyMessageSize = 1

/**
 * 8 потоков.
 * Реализовать так называемую задачу фильтрации, используя неблокирующие обмены + Waitall().
 * Если количество процессов на каждый сортирующий процесс будет неодинаковым, то появятся лишние нули.
 */
fun main(args: Array<String>) = commWorld(args) { communicator ->
    val rank = communicator.rank
    val size = communicator.size
    val sortRankList = listOf(size / 2, size - 1)

    when (rank) {
        centerRank -> {
            val workerRanksAmount = size - sortRankList.size - 1 // 1 - center rank
            val sortMessageSize = (workerRanksAmount * everyMessageSize) / sortRankList.size.toDouble()
            sortRankList
                .map { communicator.asyncReceive(size = sortMessageSize.roundToInt(), source = it) }
                .awaitAll()
                .merge()
                .sorted()
                .let { println("RESULT: $it") }
        }
        in sortRankList ->
            getReceiveRanksFor(rank, sortRankList)
                .map { communicator.asyncReceive(size = everyMessageSize, source = it) }
                .awaitAll()
                .merge()
                .sorted()
                .let { communicator.send(it, destination = centerRank) }
        else -> {
            val message = Message(everyMessageSize) { Random.nextInt(from = 1, until = 100) }
            val destination = getSortRankFor(rank, sortRankList)
            communicator
                .send(message, destination = destination)
                .let { println("SEND $message FOR $rank to $destination") }
        }
    }
}

private fun getSortRankFor(rank: Int, sortRankList: List<Int>): Int =
    sortRankList
        .mapWithPrevious { prev, curr ->
            if (rank in prev + 1 until curr) curr else null
        }
        .filterNotNull()
        .singleOrNull()
        ?: sortRankList.first()

private fun getReceiveRanksFor(sortRank: Int, sortRankList: List<Int>): List<Int> {
    val indexOfRank = sortRankList.indexOf(sortRank)
    val indexFrom = if (indexOfRank > 0) sortRankList[indexOfRank - 1] else 0
    return (indexFrom + 1 until sortRank).toList()
}
