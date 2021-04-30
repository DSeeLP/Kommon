package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.utils.DelegateCache.getCachedDelegates
import io.netty.buffer.ByteBuf
import java.lang.Exception

interface SendablePacket: Packet {
    override val readOnly: Boolean
        get() = false

    val sendMessageId: Boolean
        get() = false

    fun write(buffer: ByteBuf) {
        val that = this
        if (readOnly) return
        try {
            val delegates = this::class.getCachedDelegates(this)
            for (delegate in delegates) {
                delegate.write(buffer)
            }
        }catch (e: Exception) {
            e.printStackTrace()
        }

    }
}