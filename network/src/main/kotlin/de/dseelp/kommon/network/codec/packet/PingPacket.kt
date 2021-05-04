package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.codec.ConnectionState

class PingPacket: ReceivablePacket, SendablePacket {
    override val state: ConnectionState = ConnectionState.ANY
    override val packetIdentifier: Int = -1

    override val sendMessageId: Boolean
        get() = true

    var time by long(System.currentTimeMillis())
    var isResponse by boolean(false)
}