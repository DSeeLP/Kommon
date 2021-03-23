package de.dseelp.kommon.network.client

import de.dseelp.kommon.network.TestReceivablePacket
import de.dseelp.kommon.network.codec.packet.PacketManager
import de.dseelp.kommon.network.utils.ChannelReadEvent
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.delay
import java.net.InetSocketAddress

suspend fun main() {
    val client = KClient()
    val eventDispatcher = client.eventDispatcher
    eventDispatcher.on<ClientChannelInitializeEvent> {
        println("Channel initializing...")
    }
    eventDispatcher.on<ClientConnectEvent> {
        println("Channel connected!")
    }
    val packetDispatcher = client.packetDispatcher
    PacketManager.registerPacket(TestReceivablePacket::class)
    packetDispatcher.on<TestReceivablePacket> {
        println("Received TestPacket")
    }
    delay(200)
    client.connect(InetSocketAddress("127.0.0.1", 8282))
}