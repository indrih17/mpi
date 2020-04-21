package labs.lab7

import data.*
import graph.*
import it
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
                .toAdjacencyMatrix(graphSize = graph.size)

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
