package graph

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
        .let { matrix ->
            val nodesData = nodes.map { it.data }
            val emptyNodes = nodesData - matrix.keys
            matrix + emptyNodes.map { first -> first to nodesData.map { it to 0 }.toMap() }
        }

fun <T> AdjacencyMatrix<T>.filterReverted(): AdjacencyMatrix<T> {
    val result = map { (first, edges) ->
        first to edges.map { (second, _) -> second to 0 }.toMutableMap()
    }.toMutableMap()
    for ((first, edgesMap) in this) {
        for ((second, cost) in edgesMap) {
            result[second]?.let { edge -> if (edge[first] == 0) edge[first] = cost }
        }
    }
    return result
}

fun <K, V> List<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
    val result = HashMap<K, V>()
    for ((first, second) in this)
        result[first] = second
    return result
}

fun <T> AdjacencyMatrix<T>.breadthFirstSearch(
    current: T,
    level: Level = 0,
    costFromPrevious: Cost = 0,
    visited: List<T> = listOf(current)
): Map<T, PathData> =
    someFirstSearch(
        current = current,
        level = level,
        costFromPrevious = costFromPrevious,
        visited = visited,
        calcNewVisited = { _, visitedNodes, unvisitedAtLevel -> visitedNodes + unvisitedAtLevel }
    )

fun <T> AdjacencyMatrix<T>.depthFirstSearch(
    current: T,
    level: Level = 0,
    costFromPrevious: Cost = 0,
    visited: List<T> = listOf(current)
): Map<T, PathData> =
    someFirstSearch(
        current = current,
        level = level,
        costFromPrevious = costFromPrevious,
        visited = visited,
        calcNewVisited = { currentNode, visitedNodes, _ -> visitedNodes + currentNode }
    )

private fun <T> AdjacencyMatrix<T>.someFirstSearch(
    current: T,
    level: Level = 0,
    costFromPrevious: Cost = 0,
    visited: List<T> = listOf(current),
    calcNewVisited: (
        current: T,
        visited: List<T>,
        unvisited: List<T>
    ) -> List<T>
): Map<T, PathData> {
    val unvisitedAtLevel = getValue(current)
        .mapNotNull { (node, cost) -> if (cost != 0) node to cost else null }
        .filterNot { (node, _) -> visited.contains(node) }
    val unvisitedNodes = unvisitedAtLevel.map { (node, _) -> node }
    val result = mapOf(current to PathData(level, costFromPrevious))
    return unvisitedAtLevel
        .map { (node, cost) ->
            someFirstSearch(
                current = node,
                level = level + 1,
                costFromPrevious = cost + costFromPrevious,
                visited = calcNewVisited(node, visited, unvisitedNodes),
                calcNewVisited = calcNewVisited
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

fun <T> AdjacencyMatrix<T>.isTree(current: T): Boolean {
    val edgesCount = filterReverted().map { (_, edges) -> edges.count { (_, cost) -> cost > 0 } }.sum()
    return depthFirstSearch(current).size == size && edgesCount == size - 1
}
