package de.dseelp.kommon.network.server

import de.dseelp.kommon.event.EventDispatcher
import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.utils.KNettyUtils
import de.dseelp.kommon.network.utils.KNettyUtils.TransportType
import de.dseelp.kommon.network.utils.NetworkAddress
import de.dseelp.kommon.network.utils.awaitSuspending
import de.dseelp.kommon.network.utils.scope
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.local.LocalAddress
import io.netty.channel.local.LocalServerChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.util.concurrent.GlobalEventExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

class KServer(
    val address: NetworkAddress,
    val isNativeTransportEnabled: Boolean = true,
    isResponseEnabled: Boolean = true,
    val ssl: Boolean = true
) : PacketDispatcher(isResponseEnabled) {
    val bossGroup: EventLoopGroup = TransportType(isNativeTransportEnabled).eventLoopGroup
    val workerGroup: EventLoopGroup = TransportType(isNativeTransportEnabled).eventLoopGroup
    val socketAddress = when (address) {
        is NetworkAddress.InetNetworkAddress -> InetSocketAddress(address.host, address.port)
        is NetworkAddress.LocalNetworkAddress -> LocalAddress(address.id)
    }
    val bootstrap: ServerBootstrap = ServerBootstrap()
        .channel(if (socketAddress is LocalAddress) LocalServerChannel::class.java else TransportType(isNativeTransportEnabled).serverSocketChannel.java)
        .group(bossGroup, workerGroup)
        .childHandler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                var sslContext: SslContext? = null
                if (ssl) {
                    val ssc = SelfSignedCertificate()
                    sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
                }
                val pipeline = ch.pipeline()
                if (sslContext != null) pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc()))
                channels.add(ch)
                pipeline.addLast("defaultActive", KNettyUtils.DefaultChannelActiveHandler(true, this@KServer))
                KNettyUtils.channelPipeline(pipeline, this@KServer, isResponseEnabled)
                ch.scope.launch {
                    callEvent(ServerChannelInitializeEvent(ch))
                }
                pipeline.addLast("defaultInactive", KNettyUtils.DefaultChannelInactiveHandler(true, this@KServer))
            }
        })
    lateinit var channel: Channel
        private set

    val channels: ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    private var binding = false
    var isStopped = false
        private set
    var isStarted = false
        private set


    suspend fun start(): ChannelFuture? {
        if (binding || isStarted || isStopped) return null
        binding = true
        bootstrap.bind(socketAddress).awaitSuspending().apply {
            if (isSuccess) {
                channel = this.channel()
                isStarted = true
            } else {
                bossGroup.shutdownGracefully()
                workerGroup.shutdownGracefully()
            }
            binding = false
            callEvent(ServerBindEvent(isSuccess, cause()))
            return this
        }
    }

    suspend fun stop() {
        if (!isStarted || isStopped) return
        try {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        } finally {
            channel?.closeFuture()?.awaitSuspending()
            isStarted = false
            callEvent(ServerClosedEvent())
            coroutineScope.cancel("Server stopped")
        }
    }
}