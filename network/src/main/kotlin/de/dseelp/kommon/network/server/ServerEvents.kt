package de.dseelp.kommon.network.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.SocketChannel

data class ServerBindEvent(val isSuccess: Boolean, val cause: Throwable? = null)
enum class ServerBindFailedCause() {
    // NIO: java.net.BindException EPOLL: Errors.NativeIoException
    ADDRESS_ALREADY_IN_USE
}

class ServerClosedEvent

data class ServerChannelInitializeEvent(val channel: SocketChannel)
data class ServerChannelActiveEvent(val ctx: ChannelHandlerContext) {
    val channel = ctx.channel()!!
}

data class ServerChannelInactiveEvent(val ctx: ChannelHandlerContext) {
    val channel = ctx.channel()!!
}