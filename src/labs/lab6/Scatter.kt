package labs.lab6

import data.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTimedValue

fun scatter(args: Array<String>, vectorSize: Int): Either<Failure, Duration>? {
    commWorld(args) { communicator ->
        val vector1 = Message(vectorSize) { Random.nextInt(1, 10) }
        val vector2 = Message(vectorSize) { Random.nextInt(1, 10) }

        val timedResult = measureTimedValue {
            val subMsg1 = communicator.scatter(vector1, centerRank)
            val subMsg2 = communicator.scatter(vector2, centerRank)
            val result = (subMsg1 * subMsg2).sum()
            communicator
                .gather(messageOf(result), centerRank)
                .sum()
        }

        if (communicator.rank == centerRank) {
            val normalResult = (vector1 * vector2).sum()
            return if (timedResult.value == normalResult)
                Either.Right(timedResult.duration)
            else
                Either.Left(Failure(expected = normalResult, received = timedResult.value))
        }
    }
    return null
}
