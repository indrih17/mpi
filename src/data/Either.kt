package data

import kotlin.time.TimedValue

sealed class Either<out A, out B> {
    data class Left<T>(val value: T) : Either<T, Nothing>()
    data class Right<T>(val value: T) : Either<Nothing, T>()

    inline infix fun <C> map(f: (B) -> C): Either<A, C> = when (this) {
        is Left -> this
        is Right -> Right(f(value))
    }

    inline fun <C> fold(ifLeft: (A) -> C, ifRight: (B) -> C): C = when (this) {
        is Left -> ifLeft(value)
        is Right -> ifRight(value)
    }
}

data class Failure<T>(val expected: T, val received: T)

typealias ProgramResult<T> = Either<Failure<T>, TimedValue<T>>
