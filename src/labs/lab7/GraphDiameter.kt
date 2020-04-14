package labs.lab7

import data.*
import it
import kotlin.time.Duration
import kotlin.time.measureTimedValue

fun graphDiameter(args: Array<String>, graphSize: Int): Either<Failure, Duration>? {
    val origin = randomIntGraph(graphSize).adjacencyMatrix()
    commWorld(args) { communicator ->
        val rank = communicator.rank
        val commInfo = CommunicationInfo(communicator, origin.size)

        val timedResult = measureTimedValue {
            val message = origin.toMessage()
            val matrix = communicator
                .broadcast(message, centerRank)
                .toAdjacencyMatrix(graphSize)

            val maxValue = if (rank in commInfo.receivingRanks) {
                commInfo
                    .rangeForRank
                    .map(matrix::maxCost)
                    .mapNotNull(::it)
                    .maxBy(::it)
            } else {
                null
            }
            communicator.reduce(messageOf(maxValue ?: 0), centerRank, operation = Operation.Max).max() ?: 0
        }
        if (rank == centerRank) {
            val normalResult = origin.diameter()
            return if (timedResult.value == normalResult)
                Either.Right(timedResult.duration)
            else
                Either.Left(Failure(expected = normalResult, received = timedResult.value))
        }
    }
    return null
}

private fun <T> AdjacencyMatrix<T>.diameter(): Cost =
    keys.mapNotNull { node -> maxCost(node) }.maxBy { it } ?: 0

private fun Map<Int, Int>.flatten(): List<Int> {
    val result = ArrayList<Int>()
    for ((k, v) in this) {
        result.add(k)
        result.add(v)
    }
    return result
}

private fun AdjacencyMatrix<Int>.toMessage(): Message =
    map { (first, map) -> listOf(first) + map.flatten() }
        .flatten()
        .toIntArray()

private fun Message.toAdjacencyMatrix(graphSize: Int): AdjacencyMatrix<Int> =
    split(1 + graphSize * 2) { arr ->
        arr.first() to arr
            .drop(1)
            .toIntArray()
            .split(2) { (node, cost) -> node to cost }
            .toMap()
    }
        .toMap()