package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.utils.KNettyUtils.getDelegatedData
import io.netty.buffer.ByteBuf

interface SendablePacket: Packet {
    override val readOnly: Boolean
        get() = false

    fun write(buffer: ByteBuf) {
        if (readOnly) return
        for (delegate in this::class.getDelegatedData(this)) {
            delegate.write(buffer)
        }
    }
}