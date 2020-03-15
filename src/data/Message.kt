package data

import mpi.Request

fun messageOf(vararg elements: Int) = Message(intArrayOf(*elements))

inline class Message(val intArray: IntArray) {
    constructor(size: Int, init: (Int) -> Int = { 0 }) : this(IntArray(size, init))

    val size: Int get() = intArray.size

    fun sorted(): Message =
        Message(intArray.sortedArray())

    override fun toString(): String =
        intArray.toList().toString()

    operator fun plus(other: Message): Message =
        Message(intArray + other.intArray)

    operator fun get(index: Int): Int =
        intArray[index]

    operator fun set(index: Int, elem: Int) {
        intArray[index] = elem
    }
}

fun List<Message>.merge(): Message =
    fold(messageOf()) { acc, message -> acc + message }

fun List<Pair<Message, Request>>.awaitAll(): Message =
    map { (message, request) -> request.Wait(); message }.merge()
