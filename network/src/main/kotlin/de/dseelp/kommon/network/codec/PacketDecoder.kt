package de.dseelp.kommon.network.codec

import de.dseelp.kommon.network.codec.packet.BufferUtils.readUniqueId
import de.dseelp.kommon.network.codec.packet.BufferUtils.readVarInt
import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.utils.internalInfo
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.createInstance

class PacketDecoder(val packetDispatcher: PacketDispatcher): ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, result: MutableList<Any>) {
        if (buffer.readableBytes() > 0) {
            val packetIdentifier = buffer.readVarInt()
            val clazz = packetDispatcher.getPacketClass(ctx.channel().internalInfo.state, packetIdentifier)
            val channel = ctx.channel()
            if (clazz == null) {
                runBlocking {
                    packetDispatcher.callEvent(UnknownPacketReceivedEvent(packetIdentifier, channel.internalInfo.state, channel), true)
                }
            }else {
                val enabled = packetDispatcher.isResponseEnabled
                val messageId = if (buffer.readBoolean() && enabled) {
                    buffer.readUniqueId()
                } else null
                val responseId = if (buffer.readBoolean()  && enabled) {
                    buffer.readUniqueId()
                } else null
                val packet = clazz.createInstance()
                packet.read(buffer)
                if (responseId != null) result.add(Triple(packet, messageId, responseId))
                else result.add(packet to messageId)
            }
        }
    }

}