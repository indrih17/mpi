package labs.lab5

import labs.lab5.variations.*

private const val blockingVarName = "Blocking"
private const val bufferBlockingVarName = "Buffer blocking"
private const val notBlockingVarName = "Not blocking"
private const val readyBlockingVarName = "Ready blocking"
private const val synchronousBlockingVarName = "Sync blocking"

fun main(args: Array<String>) {
    val vectorsSize = 100_000
    val variationName = bufferBlockingVarName

    val variations = mapOf(
        blockingVarName to ::blocking,
        bufferBlockingVarName to ::bufferBlocking,
        notBlockingVarName to ::notBlocking,
        readyBlockingVarName to ::readyBlocking,
        synchronousBlockingVarName to ::synchronousBlocking
    )
    variations
        .filterKeys { it == variationName }
        .mapNotNull { (title, variation) ->
            variation(args, vectorsSize)?.let { duration -> "$title: ${duration.inMilliseconds}" }
        }
        .forEach(::println)
}
