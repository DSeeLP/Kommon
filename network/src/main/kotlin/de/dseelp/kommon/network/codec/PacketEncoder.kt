package de.dseelp.kommon.network.codec

import de.dseelp.kommon.network.codec.packet.BufferUtils.writeUniqueId
import de.dseelp.kommon.network.codec.packet.BufferUtils.writeVarInt
import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.codec.packet.SendablePacket
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class PacketEncoder(val packetDispatcher: PacketDispatcher): MessageToByteEncoder<SendablePacket>() {
    override fun encode(ctx: ChannelHandlerContext, packet: SendablePacket, buffer: ByteBuf) {
        buffer.writeVarInt(packet.packetIdentifier)
        if (packetDispatcher.isResponseEnabled) {
            val sendMessageId = packet.sendMessageId
            buffer.writeBoolean(sendMessageId)
            if (sendMessageId) buffer.writeUniqueId(packetDispatcher.getIdForSendablePacket(packet))
            buffer.writeBoolean(false)
        }
        packet.write(buffer)
    }
}