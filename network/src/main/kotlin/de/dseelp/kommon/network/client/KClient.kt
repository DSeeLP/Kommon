package de.dseelp.kommon.network.client

import de.dseelp.kommon.event.EventDispatcher
import de.dseelp.kommon.network.server.ServerClosedEvent
import de.dseelp.kommon.network.utils.KNettyUtils
import de.dseelp.kommon.network.utils.awaitSuspending
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.SocketAddress

class KClient(val nativeTransport: Boolean = true, val nThreads: Int = 0, val eventDispatcher: EventDispatcher = EventDispatcher(), val ssl: Boolean = true) {
    val loopGroup: EventLoopGroup = KNettyUtils.EventLoops.eventLoopGroup(nativeTransport, nThreads)
    val bootstrap: Bootstrap = Bootstrap()
        .channel(KNettyUtils.Channels.socketChannel(nativeTransport).java)
        .group(loopGroup)
    var channel: Channel? = null
        private set

    private var onceBinded = false

    private var binding = false
    var isBinded = false
        private set

    val packetDispatcher = EventDispatcher()


    suspend fun connect(address: SocketAddress) {
        if (binding) return
        binding = true
        bootstrap.handler(object: ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                var sslContext: SslContext? = null
                if (ssl) {
                    sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                }
                val pipeline = ch.pipeline()
                address as InetSocketAddress
                if (sslContext != null) pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc(), address.hostName, address.port))
                pipeline.addLast("defaultActive", KNettyUtils.DefaultChannelActiveHandler(false, eventDispatcher))
                KNettyUtils.channelPipeline(pipeline, eventDispatcher, packetDispatcher)
                runBlocking {
                    eventDispatcher.call(ClientChannelInitializeEvent(ch))
                }
                pipeline.addLast("defaultInactive", KNettyUtils.DefaultChannelInactiveHandler(false, eventDispatcher))
            }
        })
        bootstrap.connect(address).awaitSuspending().apply {
            if (isSuccess) {
                isBinded = true
                onceBinded = true
                channel = channel()
            }
            binding = false
            eventDispatcher.call(ClientConnectEvent(isSuccess))
        }
    }

    suspend fun shutdown() {
        if (!isBinded) return
        try {
            loopGroup.shutdownGracefully()
        }finally {
            channel?.closeFuture()?.awaitSuspending()
            isBinded = false
            eventDispatcher.call(ServerClosedEvent())
        }
    }
}