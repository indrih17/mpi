package labs.lab8

import graph.*
import kotlin.random.Random

fun main(args: Array<String>) {
    val graphSize = 1000
    val graph = randomTreeGraph(oriented = false, size = graphSize)
    treeCheck(args, graph)?.let { either ->
        println(
            either.fold(
                ifLeft = { "Ошибка: ${it.expected} vs ${it.received}" },
                ifRight = { "Duration: ${it.duration.inMilliseconds}, ответ: ${it.value}" }
            )
        )
    }
}

fun randomTreeGraph(oriented: Boolean, size: Int): Graph<Int> {
    val from = mutableListOf(0)
    val to = (1 until size).toMutableList()
    fun randomEdge(): Edge<Int> =
        Edge(
            first = Node(from.random()),
            second = Node(to.random().also { from.add(it); to.remove(it) }),
            cost = Random.nextInt(1, 10)
        )
    return createGraph(oriented = oriented, edges = (0 until size - 1).map { randomEdge() }.toSet())
}