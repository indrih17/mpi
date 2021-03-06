package labs.lab4

import data.Communicator
import data.commWorld
import data.messageOf

fun main(args: Array<String>) = commWorld(args) { communicator ->
    when (communicator.rank) {
        0 -> communicator.send(messageOf(2016), destination = 2)
        1 -> communicator.send(messageOf(1, 3, 5), destination = 2)
        2 -> {
            printReceiveWithProbe(communicator, source = 0)
            printReceiveWithProbe(communicator, source = 1)
        }
    }
}

private fun printReceiveWithProbe(communicator: Communicator, source: Int) {
    val message = communicator.receive(source = source)
    println("Rank = $source received: ${message.contentToString()}")
}
