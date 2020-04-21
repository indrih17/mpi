package graph

import java.util.LinkedList
import java.util.Queue
import kotlin.collections.ArrayList

typealias Cost = Int
typealias Level = Int

fun <T> createGraph(oriented: Boolean, edges: Set<Edge<T>>): Graph<T> =
    Graph(
        oriented = oriented,
        edges = (edges + if (!oriented) edges.map { it.reverted() } else emptyList()).toSet(),
        nodes = (edges.map { it.first } + edges.map { it.second }).toSet()
    )

data class Graph<T>(
    val oriented: Boolean,
    val edges: Set<Edge<T>>,
    val nodes: Set<Node<T>>
) {
    val size: Int = nodes.size
}
data class Node<T>(val data: T)
data class Edge<T>(val first: Node<T>, val second: Node<T>, val cost: Cost)
data class PathData(val level: Level, val cost: Cost)

fun <T> Edge<T>.reverted() = copy(first = second, second = first)
infix fun <T> Edge<T>.isInclude(node: Node<T>) = node == first || node == second
infix fun <T> Edge<T>.adjacentOrNull(node: Node<T>): Node<T>? =
    if (node != first) first else if (node != second) second else null

fun <T> Collection<Edge<T>>.filterReverted(): List<Edge<T>> {
    val result = ArrayList<Edge<T>>(size / 2)
    for (edge in this) {
        if (!result.contains(edge.reverted()))
            result.add(edge)
    }
    return result
}

fun <T> Graph<T>.plus(node: Node<T>) = copy(nodes = nodes.plus(node))
fun <T> Graph<T>.plus(first: Node<T>, second: Node<T>, cost: Cost): Graph<T> =
    copy(
        nodes = nodes.plus(first).plus(second),
        edges = edges
            .plus(Edge(first, second, cost))
            .let { if (!oriented) it.plus(Edge(second, first, cost)) else it }
    )

fun <T> Graph<T>.diameter(): Cost =
    nodes.mapNotNull { node -> maxCost(node) }.maxBy { it } ?: 0

fun <T> Graph<T>.maxCost(currentNode: Node<T>): Int? =
    breadthFirstSearch(currentNode)
        .maxBy { (_, pathData) -> pathData.cost }
        ?.let { (_, pathData) -> pathData.cost }

fun <T> Graph<T>.breadthFirstSearch(current: Node<T>): Map<Node<T>, PathData> {
    val visited = hashSetOf(current)
    val queue: Queue<Node<T>> = LinkedList<Node<T>>().also { it.add(current) }
    val result = hashMapOf(current to PathData(0, 0))
    var subNodesLevel = 0
    while (queue.isNotEmpty()) {
        val node = queue.poll()
        subNodesLevel++
        val unvisitedAtLevel = edges(node = node)
            .mapNotNull { edge -> (edge adjacentOrNull current)?.let { it to edge.cost } }
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

fun <T> Graph<T>.depthFirstSearch(
    current: Node<T>,
    level: Level = 0,
    costFromPrevious: Cost = 0,
    visited: List<Node<T>> = listOf(current)
): Map<Node<T>, PathData> {
    val unvisitedAtLevel = edges(node = current)
        .mapNotNull { edge -> (edge adjacentOrNull current)?.let { it to edge.cost } }
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

fun <T> Graph<T>.isTree(current: Node<T>): Boolean =
    depthFirstSearch(current).size == nodes.size && (if (oriented) edges.size else edges.size / 2) == nodes.size - 1

fun <T> Graph<T>.edges(node: Node<T>) = edges.filter { edge -> edge isInclude node }
