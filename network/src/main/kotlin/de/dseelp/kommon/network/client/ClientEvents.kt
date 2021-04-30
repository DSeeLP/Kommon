package de.dseelp.kommon.network.client

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.SocketChannel

data class ClientConnectEvent(val isSuccess: Boolean, val channel: Channel)
data class ClientChannelInitializeEvent(val channel: Channel)
data class ClientChannelInactiveEvent(val ctx: ChannelHandlerContext)
data class ClientChannelActiveEvent(val ctx: ChannelHandlerContext)
