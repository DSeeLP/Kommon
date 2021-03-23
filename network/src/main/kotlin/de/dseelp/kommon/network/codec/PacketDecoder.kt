package de.dseelp.kommon.network.codec

import de.dseelp.kommon.event.EventDispatcher
import de.dseelp.kommon.network.codec.packet.BufferUtils
import de.dseelp.kommon.network.codec.packet.Packet
import de.dseelp.kommon.network.codec.packet.PacketManager
import de.dseelp.kommon.network.utils.internalInfo
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.createInstance

class PacketDecoder(val eventDispatcher: EventDispatcher): ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, result: MutableList<Any>) {
        if (buffer.readableBytes() > 0) {
            val packetIdentifier = BufferUtils.readVarInt(buffer)
            val data = buffer
            val clazz = PacketManager.getPacketClass(ctx.channel().internalInfo.state, packetIdentifier)
            val channel = ctx.channel()
            if (clazz == null) {
                runBlocking {
                    eventDispatcher.call(UnknownPacketReceivedEvent(packetIdentifier, channel.internalInfo.state, channel), true)
                }
            }else {
                val packet = clazz.createInstance()
                packet.read(data)
                result.add(packet)
            }
        }
    }

}