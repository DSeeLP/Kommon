package de.dseelp.kommon.network.codec

import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.codec.packet.ReceivablePacket
import de.dseelp.kommon.network.utils.scope
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class PacketHandler(val packetDispatcher: PacketDispatcher): ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        ctx.scope.launch {
            if (msg::class == Triple::class) {
                @Suppress("UNCHECKED_CAST")
                msg as Triple<ReceivablePacket, UUID, UUID>
                packetDispatcher.call(ctx, msg.first, msg.second, msg.third)
            } else if (msg::class == Pair::class) {
                @Suppress("UNCHECKED_CAST")
                msg as Pair<ReceivablePacket, UUID>
                packetDispatcher.call(ctx, msg.first, msg.second)
            }
        }
    }
}