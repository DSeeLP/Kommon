package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.codec.ConnectionState
import de.dseelp.kommon.network.utils.KNettyUtils.getDelegatedData
import io.netty.buffer.ByteBuf

interface ReceivablePacket: Packet {
    val state: ConnectionState
    override val readOnly: Boolean
        get() = true

    fun read(buffer: ByteBuf) {
        if (!readOnly) return
        for (delegate in this::class.getDelegatedData(this)) {
            delegate.read(buffer)
        }
    }
}