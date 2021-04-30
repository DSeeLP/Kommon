package de.dseelp.kommon.network.client

import de.dseelp.kommon.network.codec.ConnectionState
import de.dseelp.kommon.network.codec.packet.ReceivablePacket
import de.dseelp.kommon.network.codec.packet.SendablePacket

class TestReceivablePacket: ReceivablePacket {
    override val state: ConnectionState
        get() = ConnectionState.DEFAULT
    override val packetIdentifier: Int
        get() = 0x01

    var time by long()
    var number by int()
}

class TestSendablePacket(id: Int, time: Long): SendablePacket {
    override val packetIdentifier: Int
        get() = 0x1

    override val sendMessageId: Boolean
        get() = true

    val time by long(time)
    val number by int(id)
}
