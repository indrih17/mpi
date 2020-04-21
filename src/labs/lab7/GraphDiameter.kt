package labs.lab7

import data.*
import flatten
import graph.*
import it
import split
import kotlin.time.Duration
import kotlin.time.measureTimedValue

fun graphDiameter(args: Array<String>, graph: Graph<Int>): Either<Failure<Int>, Duration>? {
    commWorld(args) { communicator ->
        val rank = communicator.rank
        val commInfo = CommInfo(communicator, graph.size, centralRankCollectsData = false)

        val timedResult = measureTimedValue {
            val message = graph.adjacencyMatrix().toMessage()
            val matrix = communicator
                .broadcast(message, centerRank)
                .toAdjacencyMatrix(graph.size)

            val maxValue = if (rank in commInfo.receivingRanks) {
                commInfo
                    .rangeForRank(rank)
                    .map(matrix::maxCost)
                    .mapNotNull(::it)
                    .maxBy(::it)
            } else {
                null
            }
            communicator.reduce(messageOf(maxValue ?: 0), centerRank, operation = Operation.Max).max() ?: 0
        }
        if (rank == centerRank) {
            val normalResult = graph.diameter()
            return if (timedResult.value == normalResult)
                Either.Right(timedResult.duration)
            else
                Either.Left(Failure(expected = normalResult, received = timedResult.value))
        }
    }
    return null
}

private fun AdjacencyMatrix<Int>.toMessage(): Message =
    map { (first, map) -> listOf(first) + map.flatten() }
        .flatten()
        .toIntArray()

private fun Message.toAdjacencyMatrix(graphSize: Int): AdjacencyMatrix<Int> =
    asIterable()
        .split(1 + graphSize * 2) { arr ->
            arr.first() to arr
                .drop(1)
                .split(2) { (node, cost) -> node to cost }
                .toMap()
        }
        .toMap()