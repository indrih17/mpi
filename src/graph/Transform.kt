package graph

import data.Message
import flatten

fun <T> Graph<T>.adjacencyMatrix(): AdjacencyMatrix<T> =
    edges
        .groupBy { it.first }
        .map { (node, edgesList) ->
            val values = nodes.map { second ->
                second.data to (edgesList.firstOrNull { it.second == second }?.cost ?: 0)
            }
            node.data to values.toMap()
        }
        .toMap()
        .let { matrix ->
            val nodesData = nodes.map { it.data }
            val emptyNodes = nodesData - matrix.keys
            matrix + emptyNodes.map { first -> first to nodesData.map { it to 0 }.toMap() }
        }

fun AdjacencyMatrix<Int>.toMessageList(): List<Message> =
    values.map { map -> map.flatten().toIntArray() }

fun Message.toEdgesMap(): Map<Int, Cost> =
    toList()
        .chunked(2) { (node, cost) -> node to cost }
        .toMap()
