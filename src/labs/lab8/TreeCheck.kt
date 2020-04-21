package labs.lab8

import data.*
import graph.*
import isEachItemUnique
import toIndexedArray
import kotlin.time.Duration
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

private val errorMsg = listOf(-1)

fun treeCheck(args: Array<String>, graph: Graph<Int>): Either<Failure<Boolean>, Duration>? {
    commWorld(args) { communicator ->
        val rank = communicator.rank
        val commInfo = CommInfo(communicator, graph.size, centralRankCollectsData = true)

        val timedResult: TimedValue<Boolean?> = measureTimedValue {
            if (communicator.numberOfRanks == 1) {
                graph.isTree(graph.nodes.first())
            } else {
                when (rank) {
                    centerRank -> {
                        val matrix: Iterable<Message> = graph
                            .adjacencyMatrix()
                            .map { (first, edges) -> first to edges.toIndexedArray().toIntArray() }
                            .toMap()
                            .toIndexedArray()
                            .asIterable()

                        commInfo.split(matrix).map { (rank, msgArray) ->
                            msgArray.map { communicator.send(it, destination = rank) }
                        }

                        commInfo
                            .receivingRanks
                            .map { communicator.receive(source = it).toList() }
                            .flatten()
                            .let { list -> list != errorMsg && list.isEachItemUnique() }
                    }

                    in commInfo.receivingRanks -> {
                        val visitedNodes = commInfo
                            .rangeForRank(rank)
                            .map { communicator.receive(source = centerRank) }
                            .map { msg ->
                                msg
                                    .mapIndexed { index, cost -> index to cost }
                                    .mapNotNull { (node, cost) -> if (cost != 0) node else null }
                                    .let { if (it.isEachItemUnique()) it else errorMsg }
                            }
                            .flatten()
                        communicator.send(visitedNodes.toMessage(), centerRank)
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
                Either.Right(timedResult.duration)
            else
                Either.Left(Failure(expected = normalResult, received = timedResult.value!!))
        }
    }
    return null
}
