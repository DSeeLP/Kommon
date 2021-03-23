package de.dseelp.kommon.network.client

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.SocketChannel

data class ClientConnectEvent(val isSuccess: Boolean)
data class ClientChannelInitializeEvent(val channel: SocketChannel)
data class ClientChannelInactiveEvent(val ctx: ChannelHandlerContext)
data class ClientChannelActiveEvent(val ctx: ChannelHandlerContext)
