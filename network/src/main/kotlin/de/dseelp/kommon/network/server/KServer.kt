package de.dseelp.kommon.network.server

import de.dseelp.kommon.event.EventDispatcher
import de.dseelp.kommon.network.utils.KNettyUtils
import de.dseelp.kommon.network.utils.awaitSuspending
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.util.concurrent.GlobalEventExecutor
import kotlinx.coroutines.runBlocking

class KServer(val nativeTransport: Boolean = true, val nThreads: Int = 0, val eventDispatcher: EventDispatcher = EventDispatcher(), val ssl: Boolean = true) {
    val mainGroup: EventLoopGroup = KNettyUtils.EventLoops.eventLoopGroup(nativeTransport, nThreads)
    val childGroup: EventLoopGroup = KNettyUtils.EventLoops.eventLoopGroup(nativeTransport, nThreads)
    val bootstrap: ServerBootstrap = ServerBootstrap()
        .channel(KNettyUtils.Channels.serverSocketChannel(nativeTransport).java)
        .group(mainGroup, childGroup)
        .childHandler(object: ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                var sslContext: SslContext? = null
                if (ssl) {
                    val ssc = SelfSignedCertificate()
                    sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
                }
                val pipeline = ch.pipeline()
                if (sslContext != null) pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc()))
                channels.add(ch)
                pipeline.addLast("defaultActive", KNettyUtils.DefaultChannelActiveHandler(true, eventDispatcher))
                KNettyUtils.channelPipeline(pipeline, eventDispatcher, packetDispatcher)
                runBlocking {
                    eventDispatcher.call(ServerChannelInitializeEvent(ch))
                }
                pipeline.addLast("defaultInactive", KNettyUtils.DefaultChannelInactiveHandler(true, eventDispatcher))
            }
        })
    var channel: Channel? = null
    private set

    val channels: ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    private var onceBinded = false

    private var binding = false
    var isBinded = false
    private set

    val packetDispatcher = EventDispatcher()


    suspend fun bind(host: String = "0.0.0.0", port: Int): ChannelFuture? {
        if (binding) return null
        binding = true
        bootstrap.bind(host, port).awaitSuspending().apply {
            if (isSuccess) {
                isBinded = true
                onceBinded = true
            }else {
                mainGroup.shutdownGracefully()
                childGroup.shutdownGracefully()
            }
            binding = false
            eventDispatcher.call(ServerBindEvent(isSuccess, cause()))
            return this
        }
    }

    suspend fun shutdown() {
        if (!isBinded) return
        try {
            mainGroup.shutdownGracefully()
            childGroup.shutdownGracefully()
        }finally {
            channel?.closeFuture()?.awaitSuspending()
            isBinded = false
            eventDispatcher.call(ServerClosedEvent())
        }
    }
}