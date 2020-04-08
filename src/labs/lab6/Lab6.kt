package labs.lab6

import kotlin.time.Duration

fun main(args: Array<String>) {
    val vectorSize = 1000
    scatter(args, vectorSize)?.let { either ->
        println(
            either.fold(
                ifLeft = { "Ошибка!" },
                ifRight = Duration::inMilliseconds
            )
        )
    }
}
