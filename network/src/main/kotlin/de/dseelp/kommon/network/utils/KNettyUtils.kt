package de.dseelp.kommon.network.utils

import de.dseelp.kommon.network.client.ClientChannelActiveEvent
import de.dseelp.kommon.network.client.ClientChannelInactiveEvent
import de.dseelp.kommon.network.codec.*
import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.codec.ResponsePacketEncoder
import de.dseelp.kommon.network.server.ServerChannelActiveEvent
import de.dseelp.kommon.network.server.ServerChannelInactiveEvent
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPipeline
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

object KNettyUtils {

    var DEBUG_MODE = false

    fun channelPipeline(
        pipeline: ChannelPipeline,
        packetDispatcher: PacketDispatcher,
        isResponseEnabled: Boolean
    ) {
        pipeline.addLast("packetFramer", PacketFramer())
        pipeline.addLast("packetDecoder", PacketDecoder(packetDispatcher))
        pipeline.addLast("packetEncoder", PacketEncoder(packetDispatcher))
        if (isResponseEnabled) pipeline.addLast("responsePacketEncoder", ResponsePacketEncoder(packetDispatcher))
        pipeline.addLast("packetHandler", PacketHandler(packetDispatcher))
    }

    class DefaultChannelActiveHandler(val server: Boolean, val packetDispatcher: PacketDispatcher) :
        ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            runBlocking {
                if (server) {
                    packetDispatcher.callEvent(ServerChannelActiveEvent(ctx), true)
                } else packetDispatcher.callEvent(ClientChannelActiveEvent(ctx), true)
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
        }
    }
    class DefaultChannelInactiveHandler(val server: Boolean, val packetDispatcher: PacketDispatcher) :
        ChannelInboundHandlerAdapter() {
        override fun channelInactive(ctx: ChannelHandlerContext) {
            ChannelExtensions.resetChannel(ctx.channel().id())
            runBlocking {
                if (server) {
                    packetDispatcher.callEvent(ServerChannelInactiveEvent(ctx), true)
                } else packetDispatcher.callEvent(ClientChannelInactiveEvent(ctx), true)
            }
        }
    }

    enum class TransportType(
        val serverSocketChannel: KClass<out ServerSocketChannel>,
        val socketChannel: KClass<out SocketChannel>
    ) {
        KQUEUE(KQueueServerSocketChannel::class, KQueueSocketChannel::class),
        EPOLL(EpollServerSocketChannel::class, EpollSocketChannel::class),
        NIO(NioServerSocketChannel::class, NioSocketChannel::class);

        val eventLoopGroup
            get() = when (this) {
                KQUEUE -> KQueueEventLoopGroup()
                EPOLL -> EpollEventLoopGroup()
                NIO -> NioEventLoopGroup()
            }

        companion object {
            fun find(): TransportType {
                if (KQueue.isAvailable()) return KQUEUE
                if (Epoll.isAvailable()) return EPOLL
                return NIO
            }

            operator fun invoke(nativeTransport: Boolean): TransportType = if (nativeTransport) find() else NIO
            val AUTO = find()
        }
    }
}