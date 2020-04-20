package labs.lab7

import graph.*
import kotlin.random.Random
import kotlin.time.Duration

fun main(args: Array<String>) {
    val graphSize = 10
    graphDiameter(args, graphSize)?.let { either ->
        println(
            either.fold(
                ifLeft = { "Ошибка: ${it.expected} vs ${it.received}" },
                ifRight = Duration::inMilliseconds
            )
        )
    }
}

fun randomIntGraph(oriented: Boolean, size: Int): Graph<Int> {
    val nodes = (0 until size)
    fun randomNodeWithout(current: Int) =
        Node(data = nodes.minus(current).let { it[Random.nextInt(it.size)] })
    return createGraph(
        oriented = oriented,
        edges = nodes
            .map {
                Edge(
                    Node(it),
                    randomNodeWithout(it),
                    cost = Random.nextInt(1, 10)
                )
            }
            .toSet()
    )
}
