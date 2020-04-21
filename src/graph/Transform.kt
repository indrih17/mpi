package graph

import data.Message
import flatten
import split

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

fun AdjacencyMatrix<Int>.toMessage(): Message =
    map { (first, map) -> listOf(first) + map.flatten() }
        .flatten()
        .toIntArray()

fun Message.toAdjacencyMatrix(graphSize: Int): AdjacencyMatrix<Int> =
    asIterable()
        .split(1 + graphSize * 2) { arr ->
            arr.first() to arr
                .drop(1)
                .split(2) { (node, cost) -> node to cost }
                .toMap()
        }
        .toMap()