package data

typealias Cost = Int
typealias Level = Int

fun <T> createGraph(edges: Set<Edge<T>>): Graph<T> =
    Graph(
        edges = (edges + edges.map { it.reverted() }).toSet(),
        nodes = (edges.map { it.first } + edges.map { it.second }).toSet()
    )

data class Graph<T>(
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

fun <T> Graph<T>.plus(node: Node<T>) = copy(nodes = nodes.plus(node))
fun <T> Graph<T>.plus(first: Node<T>, second: Node<T>, cost: Cost): Graph<T> =
    copy(
        nodes = nodes.plus(first).plus(second),
        edges = edges.plus(Edge(first, second, cost)).plus(Edge(second, first, cost))
    )

fun <T> Graph<T>.maxCost(currentNode: Node<T>): Int? =
    breadthFirstSearch(currentNode)
        .maxBy { (pathData, _) -> pathData.cost }
        ?.let { (pathData, _) -> pathData.cost }

fun <T> Graph<T>.breadthFirstSearch(
    current: Node<T>,
    level: Level = 0,
    costFromPrevious: Cost = 0,
    visited: List<Node<T>> = listOf(current)
): Map<PathData, Node<T>> {
    val unvisited = edges(node = current)
        .filterReverted()
        .mapNotNull { edge -> (edge adjacentOrNull current)?.let { it to edge.cost } }
        .filterNot { (node, _) -> visited.contains(node) }
    val newVisited = visited + unvisited.map { (node, _) -> node }
    val result = mapOf(PathData(level, costFromPrevious) to current)
    return unvisited
        .map { (node, cost) ->
            breadthFirstSearch(
                current = node,
                level = level + 1,
                costFromPrevious = cost + costFromPrevious,
                visited = newVisited
            )
        }
        .fold(result) { acc, new -> acc + new }
}

fun <T> Graph<T>.edges(node: Node<T>) = edges.filter { edge -> edge isInclude node }

typealias AdjacencyMatrix<T> = Map<T, Map<T, Cost>>

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

fun <T> AdjacencyMatrix<T>.breadthFirstSearch(
    current: T,
    level: Level = 0,
    costFromPrevious: Cost = 0,
    visited: List<T> = listOf(current)
): Map<PathData, T> {
    val unvisited = getValue(current)
        .mapNotNull { (node, cost) -> if (cost != 0) node to cost else null }
        .filterNot { (node, _) -> visited.contains(node) }
    val newVisited = visited + unvisited.map { (node, _) -> node }
    val result = mapOf(PathData(level, costFromPrevious) to current)
    return unvisited
        .map { (node, cost) ->
            breadthFirstSearch(
                current = node,
                level = level + 1,
                costFromPrevious = cost + costFromPrevious,
                visited = newVisited
            )
        }
        .fold(result) { acc, new -> acc + new }
}

fun <T> AdjacencyMatrix<T>.maxCost(currentNode: T): Int? =
    breadthFirstSearch(currentNode)
        .maxBy { (pathData, _) -> pathData.cost }
        ?.let { (pathData, _) -> pathData.cost }

fun <T> List<Edge<T>>.filterReverted(): List<Edge<T>> {
    val result = mutableListOf<Edge<T>>()
    for (edge in this) {
        if (!result.contains(edge.reverted())) {
            result.add(edge)
        }
    }
    return result
}
