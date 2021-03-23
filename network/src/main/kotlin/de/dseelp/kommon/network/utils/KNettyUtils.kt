package de.dseelp.kommon.network.utils

import de.dseelp.kommon.event.EventDispatcher
import de.dseelp.kommon.network.client.ClientChannelActiveEvent
import de.dseelp.kommon.network.client.ClientChannelInactiveEvent
import de.dseelp.kommon.network.codec.PacketDecoder
import de.dseelp.kommon.network.codec.PacketEncoder
import de.dseelp.kommon.network.codec.PacketFramer
import de.dseelp.kommon.network.codec.PacketHandler
import de.dseelp.kommon.network.codec.packet.Packet
import de.dseelp.kommon.network.codec.packet.PacketData
import de.dseelp.kommon.network.server.ServerChannelActiveEvent
import de.dseelp.kommon.network.server.ServerChannelInactiveEvent
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
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
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

object KNettyUtils {

    fun channelPipeline(pipeline: ChannelPipeline, eventDispatcher: EventDispatcher, packetDispatcher: EventDispatcher) {
        pipeline.addLast("packetFramer", PacketFramer())
        pipeline.addLast("packetDecoder", PacketDecoder(eventDispatcher))
        pipeline.addLast("packetEncoder", PacketEncoder())
        pipeline.addLast("packetHandler", PacketHandler(packetDispatcher))
    }

    class DefaultChannelActiveHandler(val server: Boolean, val eventDispatcher: EventDispatcher) :
        ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            runBlocking {
                if (server) {
                    eventDispatcher.call(ServerChannelActiveEvent(ctx), true)
                } else eventDispatcher.call(ClientChannelActiveEvent(ctx), true)
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable) {
            cause.printStackTrace()
        }
    }
    class DefaultChannelInactiveHandler(val server: Boolean, val eventDispatcher: EventDispatcher) :
        ChannelInboundHandlerAdapter() {
        override fun channelInactive(ctx: ChannelHandlerContext) {
            ChannelExtensions.resetChannel(ctx.channel().id())
            runBlocking {
                if (server) {
                    eventDispatcher.call(ServerChannelInactiveEvent(ctx), true)
                } else eventDispatcher.call(ClientChannelInactiveEvent(ctx), true)
            }
        }
    }

    object Channels {
        fun nativeServerSocketChannelClass(): KClass<out ServerSocketChannel>? {
            if (Epoll.isAvailable()) return EpollServerSocketChannel::class
            if (KQueue.isAvailable()) return KQueueServerSocketChannel::class
            return null
        }

        fun defaultServerSocketChannel(): KClass<out ServerSocketChannel> = NioServerSocketChannel::class

        fun serverSocketChannel(nativeTransport: Boolean = true): KClass<out ServerSocketChannel> =
            (if (nativeTransport) nativeServerSocketChannelClass()
                ?: defaultServerSocketChannel() else defaultServerSocketChannel())


        fun nativeSocketChannelClass(): KClass<out SocketChannel>? {
            if (Epoll.isAvailable()) return EpollSocketChannel::class
            if (KQueue.isAvailable()) return KQueueSocketChannel::class
            return null
        }

        fun defaultSocketChannel(): KClass<out SocketChannel> = NioSocketChannel::class

        fun socketChannel(nativeTransport: Boolean = true): KClass<out SocketChannel> =
            (if (nativeTransport) nativeSocketChannelClass() ?: defaultSocketChannel() else defaultSocketChannel())

    }

    object EventLoops {
        fun nativeEventLoopGroup(nThreads: Int = 0): EventLoopGroup? {
            if (Epoll.isAvailable()) return EpollEventLoopGroup(nThreads)
            if (KQueue.isAvailable()) return KQueueEventLoopGroup(nThreads)
            return null
        }

        fun defaultEventLoopGroup(nThreads: Int = 0): EventLoopGroup = NioEventLoopGroup(nThreads)

        fun eventLoopGroup(nativeTransport: Boolean = false, nThreads: Int = 0) =
            if (nativeTransport) nativeEventLoopGroup(nThreads)
                ?: defaultEventLoopGroup(nThreads) else defaultEventLoopGroup(nThreads)
    }

    internal fun <T: Packet> KClass<T>.getDelegatedData(obj: Any) = declaredMemberProperties.onEach { it.isAccessible = true }.asSequence().map { it.getDelegate(obj as T) }.filter { it is PacketData<*, *> }.map { it as PacketData<*, *> }.toList().toTypedArray()
}