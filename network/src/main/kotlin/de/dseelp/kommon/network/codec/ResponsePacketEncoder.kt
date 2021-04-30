package de.dseelp.kommon.network.codec

import de.dseelp.kommon.network.codec.packet.BufferUtils.writeUniqueId
import de.dseelp.kommon.network.codec.packet.BufferUtils.writeVarInt
import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.codec.packet.SendablePacket
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import java.util.*

class ResponsePacketEncoder(val packetDispatcher: PacketDispatcher): MessageToByteEncoder<Pair<UUID, SendablePacket>>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Pair<UUID, SendablePacket>, buffer: ByteBuf) {
        val id = msg.first
        val packet = msg.second
        buffer.writeVarInt(packet.packetIdentifier)
        buffer.writeBoolean(true)
        buffer.writeUniqueId(packetDispatcher.getIdForSendablePacket(packet))
        buffer.writeBoolean(true)
        buffer.writeUniqueId(id)
        packet.write(buffer)
    }
}