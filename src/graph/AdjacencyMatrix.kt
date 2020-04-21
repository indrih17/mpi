package graph

import java.util.LinkedList
import java.util.Queue

typealias AdjacencyMatrix<T> = Map<T, Map<T, Cost>>

fun <T> AdjacencyMatrix<T>.breadthFirstSearch(current: T): Map<T, PathData> {
    val visited = hashSetOf(current)
    val queue: Queue<T> = LinkedList<T>().also { it.add(current) }
    val result = hashMapOf(current to PathData(0, 0))
    var subNodesLevel = 0
    while (queue.isNotEmpty()) {
        val node = queue.poll()
        subNodesLevel++
        val unvisitedAtLevel = getValue(node)
            .mapNotNull { (node, cost) -> if (cost != 0) node to cost else null }
            .filterNot { (node, _) -> visited.contains(node) }
            .map { (node, cost) -> node to PathData(subNodesLevel, cost) }
            .toMap()
        unvisitedAtLevel.keys.let {
            visited += it
            queue += it
        }
        result += unvisitedAtLevel
    }
    return result
}

fun <T> AdjacencyMatrix<T>.depthFirstSearch(
    current: T = keys.first(),
    level: Level = 0,
    costFromPrevious: Cost = 0,
    visited: List<T> = listOf(current)
): Map<T, PathData> {
    val unvisitedAtLevel = getValue(current)
        .mapNotNull { (node, cost) -> if (cost != 0) node to cost else null }
        .filterNot { (node, _) -> visited.contains(node) }
    val result = mapOf(current to PathData(level, costFromPrevious))
    return unvisitedAtLevel
        .map { (node, cost) ->
            depthFirstSearch(
                current = node,
                level = level + 1,
                costFromPrevious = cost + costFromPrevious,
                visited = visited + node
            )
        }
        .fold(result) { acc, new -> acc + new }
}

fun <T> AdjacencyMatrix<T>.maxCost(currentNode: T): Int? =
    breadthFirstSearch(currentNode)
        .maxBy { (_, pathData) -> pathData.cost }
        ?.let { (_, pathData) -> pathData.cost }

fun <T> AdjacencyMatrix<T>.diameter(): Cost =
    keys.mapNotNull { node -> maxCost(node) }.maxBy { it } ?: 0

fun <T> AdjacencyMatrix<T>.isTree(): Boolean =
    depthFirstSearch().size == size && (if (oriented()) edgesCount() else edgesCount() / 2) == size - 1

fun <T> AdjacencyMatrix<T>.oriented(): Boolean =
    any { (first, edges) -> edges.keys.any { second -> this[first]?.get(second) != this[second]?.get(first) } }

fun <T> AdjacencyMatrix<T>.edgesCount(): Int =
    map { (_, edges) -> edges.count { (_, cost) -> cost > 0 } }.sum()
