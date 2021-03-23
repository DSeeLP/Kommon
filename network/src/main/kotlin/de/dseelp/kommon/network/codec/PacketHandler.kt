package de.dseelp.kommon.network.codec

import de.dseelp.kommon.event.EventDispatcher
import de.dseelp.kommon.network.codec.packet.ReceivablePacket
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import kotlinx.coroutines.runBlocking

class PacketHandler(val packetDispatcher: EventDispatcher): ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any) {
        println("Channel read")
        runBlocking {
            packetDispatcher.call(msg)
        }
    }
}