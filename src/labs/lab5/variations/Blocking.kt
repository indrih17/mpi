package labs.lab5.variations

import data.Message
import data.centerRank
import data.commWorld
import data.merge
import data.multiple
import getLength
import data.splitWithIterationNumber
import labs.lab5.CommunicationInfo
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTimedValue

fun blocking(args: Array<String>, vectorSize: Int): Duration? {
    var time: Duration? = null
    commWorld(args) { communicator ->
        val rank = communicator.rank
        val commInfo = CommunicationInfo(communicator, vectorSize)
        when (rank) {
            centerRank -> {
                val vector1 = Message(vectorSize) { Random.nextInt(1, 10) }
                val vector2 = Message(vectorSize) { Random.nextInt(1, 10) }

                val timedResult = measureTimedValue {
                    vector1.splitWithIterationNumber(step = commInfo.subMessageSize) { iteration, msg ->
                        communicator.send(msg, destination = iteration)
                    }
                    vector2.splitWithIterationNumber(step = commInfo.subMessageSize) { iteration, msg ->
                        communicator.send(msg, destination = iteration)
                    }
                    commInfo
                        .receivingRanks
                        .map { communicator.receive(size = commInfo.subMessageSize, source = it) }
                        .merge()
                }

                val normalResult = vector1 multiple vector2
                if (timedResult.value contentEquals normalResult) {
                    time = timedResult.duration
                }
            }
            in commInfo.receivingRanks -> {
                val count = communicator.probe(source = centerRank).getLength()
                val vector1 = communicator.receive(size = count, source = centerRank)
                val vector2 = communicator.receive(size = count, source = centerRank)
                communicator.send(
                    message = vector1 multiple vector2,
                    destination = centerRank
                )
            }
            else -> println("Ранк не используется: $rank")
        }
    }
    return time
}
