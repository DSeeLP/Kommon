package de.dseelp.kommon.network.codec

import de.dseelp.kommon.network.codec.packet.BufferUtils
import de.dseelp.kommon.network.codec.packet.Packet
import de.dseelp.kommon.network.codec.packet.SendablePacket
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class PacketEncoder: MessageToByteEncoder<SendablePacket>() {
    override fun encode(ctx: ChannelHandlerContext, packet: SendablePacket, buffer: ByteBuf) {
        BufferUtils.writeVarInt(packet.packetIdentifier, buffer)
        packet.write(buffer)
    }
}