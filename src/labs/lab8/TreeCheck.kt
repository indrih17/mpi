package labs.lab8

import data.*
import graph.*
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

fun treeCheck(args: Array<String>, graph: Graph<Int>): Either<Failure<Boolean>, TimedValue<Boolean>>? {
    commWorld(args) { communicator ->
        val rank = communicator.rank
        val commInfo = CommInfo(communicator, graph.size, centralRankCollectsData = true)

        val timedResult: TimedValue<Boolean?> = measureTimedValue {
            val matrix = graph.adjacencyMatrix()
            if (communicator.numberOfRanks == 1) {
                matrix.isTree()
            } else {
                when (rank) {
                    centerRank -> {
                        val messageIterable = matrix.map { (node, edges) -> mapOf(node to edges).toMessage() }
                        commInfo.split(messageIterable).map { (rank, msgArray) ->
                            msgArray.map { communicator.send(it, destination = rank) }
                        }

                        val edgesCount = commInfo
                            .receivingRanks
                            .map { communicator.receive(source = it).single() }
                            .sum()
                            .let { if (matrix.oriented()) it else it / 2 }
                        edgesCount == graph.size - 1 && matrix.depthFirstSearch().size == graph.size
                    }

                    in commInfo.receivingRanks -> {
                        val edgesCount = commInfo
                            .rangeForRank(rank)
                            .map { communicator.receive(source = centerRank) }
                            .map { msg ->
                                msg
                                    .toAdjacencyMatrix(graph.size)
                                    .edgesCount()
                            }
                            .sum()
                        communicator.send(messageOf(edgesCount), centerRank)
                        null
                    }

                    else -> {
                        println("Ранк не используется: $rank")
                        null
                    }
                }
            }
        }
        if (rank == centerRank) {
            val normalResult = graph.isTree(graph.nodes.first())
            return if (timedResult.value == normalResult)
                Either.Right(TimedValue(value = normalResult, duration = timedResult.duration))
            else
                Either.Left(Failure(expected = normalResult, received = timedResult.value!!))
        }
    }
    return null
}
