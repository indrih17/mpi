package labs.lab6

import data.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTimedValue

fun broadcast(args: Array<String>, vectorSize: Int): Either<Failure<Int>, Duration>? {
    commWorld(args) { communicator ->
        val rank = communicator.rank
        val commInfo = CommInfo(communicator, vectorSize, centralRankCollectsData = false)

        val vector1 = Message(vectorSize) { Random.nextInt(1, 10) }
        val vector2 = Message(vectorSize) { Random.nextInt(1, 10) }

        val timedResult = measureTimedValue {
            val new1 = communicator.broadcast(vector1, centerRank)
            val new2 = communicator.broadcast(vector2, centerRank)

            val subMsg1 = new1.getSubMessageFor(rank, commInfo)
            val subMsg2 = new2.getSubMessageFor(rank, commInfo)
            val result = (subMsg1 * subMsg2).sum()

            communicator
                .reduce(messageOf(result), centerRank, operation = Operation.Sum)
                .sum()
        }

        if (rank == centerRank) {
            val normalResult = (vector1 * vector2).sum()
            return if (timedResult.value == normalResult)
                Either.Right(timedResult.duration)
            else
                Either.Left(Failure(expected = normalResult, received = timedResult.value))
        }
    }
    return null
}

private fun Message.getSubMessageFor(rank: Int, commInfo: CommInfo): Message {
    val from = commInfo.rangeForRank(rank).first
    val to = commInfo.rangeForRank(rank).last
    return copyOfRange(from, to + 1)
}
