package de.dseelp.kommon.network

import de.dseelp.kommon.network.client.ClientConnectEvent
import de.dseelp.kommon.network.client.KClient
import de.dseelp.kommon.network.codec.packet.PingPacket
import de.dseelp.kommon.network.server.KServer
import de.dseelp.kommon.network.server.ServerBindEvent
import de.dseelp.kommon.network.utils.NetworkAddress
import kotlinx.coroutines.*
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
class PingTest {
    /*companion object {
        val server = KServer(NetworkAddress.ANY)
        lateinit var client: KClient

        @BeforeClass
        @JvmStatic
        fun before() = runBlocking {
            println("Setup")
            server.onEvent<ServerBindEvent> {
                println("Server started")
            }
            launch { server.start() }
            val serverStartEvent = server.waitForEvent<ServerBindEvent>()
            serverStartEvent.await()
            assertFalse(serverStartEvent.isCancelled, "Server failed to start!")
            client = KClient(NetworkAddress.ANY)
            client.socketAddress = server.channel.localAddress()
            client.onEvent<ClientConnectEvent> {
                println("Client connected")
            }
            launch { client.connect() }
            val clientConnectEvent = server.waitForEvent<ClientConnectEvent>(10000)
            clientConnectEvent.await()
            assertFalse(clientConnectEvent.isCancelled, "Client failed to connect to server!")
        }

        @AfterClass
        @JvmStatic
        fun cleanup() = runBlocking { actualCleanup() }

        private suspend fun actualCleanup() {
            println("Cleaning up")
            server.stop()
            delay(2000)
        }
    }

    @Test
    fun test2() = runBlocking {
        client.invokeSuspend {
            val sent = sendPacket<PingPacket>(PingPacket())
            val response = sent.await()
            assertFalse(sent.isCancelled, "Ping timeout")
        }
    }*/
}