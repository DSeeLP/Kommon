package de.dseelp.kommon.network.client

import de.dseelp.kommon.network.utils.NetworkAddress
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
suspend fun main() {
    val client = KClient(NetworkAddress.inet("127.0.0.1", 3478))
    /*client.on<TestReceivablePacket> { ctx, packet ->
        println("Received TestPacket from ${ctx.channel().id().asShortText()}")
    }*/
    client.onEvent<ClientChannelInitializeEvent> {
        println("Channel initializing...")
    }
    client.onEvent<ClientConnectEvent> { event ->
        println("Channel connected!")
        coroutineScope {
            launch {
                var results = setOf<Int>()
                val took = measureTimeMillis {
                    repeat(6000) {
                        try {
                            //println("Sending ${it+1}")
                            val received =
                                event.channel.sendPacket<TestReceivablePacket>(TestSendablePacket(it+1, System.currentTimeMillis()))
                            val sendTime = received.await().time
                            val receivedTime = System.currentTimeMillis()
                            val difference = receivedTime - sendTime
                            println("${it+1}: Time: $difference")
                            if (it >= 5000) results+=difference.toInt()

                        }catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                println("Average: ${results.average()}")
                println("""
                    Cache Size:
                        Received: ${client.receivedPacketIdCache.size}
                        Channel: ${client.channelCache.size}
                        Sended: ${client.sendedPacketIdCache.size}
                """.trimIndent())
                println("Took $took")
                client.disconnect()
            }
        }
        println("exited")
    }
    client.registerPacket(TestReceivablePacket::class)

    delay(200)
    client.connect()
}