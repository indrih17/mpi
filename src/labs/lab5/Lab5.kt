package labs.lab5

import data.*
import getLength
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTimedValue

private const val blockingVarName = "Blocking"
private const val bufferBlockingVarName = "Buffer blocking"
private const val notBlockingVarName = "Not blocking"
private const val readyBlockingVarName = "Ready blocking"
private const val synchronousBlockingVarName = "Sync blocking"

private const val selectedVar = synchronousBlockingVarName
private const val vectorSize = 10000

typealias SendFunction = (comm: Communicator, msg: Message, dest: Int) -> Unit

fun main(args: Array<String>) {
    val variations: Map<String, SendFunction> = mapOf(
        blockingVarName to { comm, msg, dest -> comm.send(msg, dest) },
        bufferBlockingVarName to { comm, msg, dest -> comm.bufferSend(msg, dest) },
        notBlockingVarName to { comm, msg, dest -> comm.asyncSend(msg, dest) },
        readyBlockingVarName to { comm, msg, dest -> comm.readySend(msg, dest) },
        synchronousBlockingVarName to { comm, msg, dest -> comm.syncSend(msg, dest) }
    )
    val function = variations.getValue(selectedVar)
    mpi(args, send = function)?.let { either ->
        println("$selectedVar: ${
            either.fold(
                ifLeft = { "Ошибка!" },
                ifRight = Duration::inMilliseconds
            )
        }")
    }
}

private fun mpi(args: Array<String>, send: SendFunction): Either<Failure, Duration>? {
    commWorld(args) { communicator ->
        val rank = communicator.rank
        val commInfo = CommunicationInfo(communicator, vectorSize)
        when (rank) {
            centerRank -> {
                val vector1 = Message(vectorSize) { Random.nextInt(1, 10) }
                val vector2 = Message(vectorSize) { Random.nextInt(1, 10) }

                val timedResult = measureTimedValue {
                    vector1.splitWithIterationNumber(step = commInfo.subMessageSize) { iteration, msg ->
                        send(communicator, msg, iteration)
                    }
                    vector2.splitWithIterationNumber(step = commInfo.subMessageSize) { iteration, msg ->
                        send(communicator, msg, iteration)
                    }
                    commInfo
                        .receivingRanks
                        .map { communicator.receive(size = 1, source = it) }
                        .merge()
                        .sum()
                }

                val normalResult = (vector1 * vector2).sum()
                return if (timedResult.value == normalResult) Either.Right(timedResult.duration) else Either.Left(Failure)
            }
            in commInfo.receivingRanks -> {
                val count = communicator.probe(source = centerRank).getLength()
                val vector1 = communicator.receive(size = count, source = centerRank)
                val vector2 = communicator.receive(size = count, source = centerRank)
                send(communicator, messageOf((vector1 * vector2).sum()), centerRank)
            }
            else -> println("Ранк не используется: $rank")
        }
    }
    return null
}
