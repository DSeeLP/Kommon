package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.codec.ConnectionState
import de.dseelp.kommon.network.utils.DelegateCache.getCachedDelegates
import io.netty.buffer.ByteBuf

interface ReceivablePacket: Packet {
    val state: ConnectionState

    fun read(buffer: ByteBuf) {
        for (delegate in this::class.getCachedDelegates(this)) {
            delegate.read(buffer)
        }
    }
}