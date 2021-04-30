package de.dseelp.kommon.network.server

import de.dseelp.kommon.network.client.TestReceivablePacket
import de.dseelp.kommon.network.client.TestSendablePacket
import de.dseelp.kommon.network.codec.UnknownPacketReceivedEvent
import de.dseelp.kommon.network.utils.NetworkAddress

suspend fun main() {
    val server = KServer(NetworkAddress.inet("127.0.0.1", 3478))
    server.onEvent<ServerChannelActiveEvent> { event ->
        println("Channel connected")
    }

    server.onEvent<ServerBindEvent> {
        println("Server binded")
    }
    server.on<TestReceivablePacket> { ctx, packet ->
        println("Received ${packet.number}")
        packet.respond(TestSendablePacket(packet.number, packet.time))
    }
    server.registerPacket<TestReceivablePacket>()
    server.onEvent<UnknownPacketReceivedEvent> {
        println("Received unknown Packet with id: ${it.packetIdentifier}")
    }

    server.start()
}