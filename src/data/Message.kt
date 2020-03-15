package data

import mpi.Request

typealias Message = IntArray

fun messageOf(vararg elements: Int) = intArrayOf(*elements)

fun message(size: Int) = IntArray(size)

/** [IntArray.toString] нормального человека. */
fun Message.stringify(): String = toList().toString()

fun List<Message>.merge(): IntArray =
    fold(messageOf()) { acc, arr -> acc + arr }

fun List<Pair<Message, Request>>.awaitAll(): Message =
    map { (message, request) -> request.Wait(); message }.merge()
