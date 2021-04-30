package de.dseelp.kommon.network.client

import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.codec.packet.SendablePacket
import de.dseelp.kommon.network.server.ServerClosedEvent
import de.dseelp.kommon.network.utils.KNettyUtils
import de.dseelp.kommon.network.utils.KNettyUtils.TransportType
import de.dseelp.kommon.network.utils.NetworkAddress
import de.dseelp.kommon.network.utils.awaitSuspending
import de.dseelp.kommon.network.utils.scope
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.local.LocalAddress
import io.netty.channel.local.LocalChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

class KClient(
    val address: NetworkAddress,
    val isNativeTransportEnabled: Boolean = true,
    val ssl: Boolean = true,
    isResponseEnabled: Boolean = true
): PacketDispatcher(isResponseEnabled) {
    val eventLoopGroup: EventLoopGroup = TransportType(isNativeTransportEnabled).eventLoopGroup
    val socketAddress = when (address) {
        is NetworkAddress.InetNetworkAddress -> InetSocketAddress(address.host, address.port)
        is NetworkAddress.LocalNetworkAddress -> LocalAddress(address.id)
    }
    private val initCode: (Channel) -> Unit = { ch ->
        val pipeline = ch.pipeline()
        if (ssl && socketAddress is InetSocketAddress) {
            val sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
            pipeline.addLast("sslHandler", sslContext.newHandler(ch.alloc(), socketAddress.hostName, socketAddress.port))
        }
        pipeline.addLast("defaultActive", KNettyUtils.DefaultChannelActiveHandler(false, this))
        KNettyUtils.channelPipeline(pipeline, this@KClient, isResponseEnabled)
        ch.scope.launch {
            callEvent(ClientChannelInitializeEvent(ch))
        }
        pipeline.addLast("defaultInactive", KNettyUtils.DefaultChannelInactiveHandler(false, this))
    }
    val bootstrap: Bootstrap = Bootstrap()
        .channel(if (socketAddress is LocalAddress) LocalChannel::class.java else TransportType(isNativeTransportEnabled).socketChannel.java)
        .group(eventLoopGroup)
        .handler(object: ChannelInitializer<Channel>() {
            override fun initChannel(ch: Channel) {
                initCode.invoke(ch)
            }
        })
    lateinit var channel: Channel
        private set

    private var isStopped = false

    private var binding = false
    var isConnected = false
        private set

    fun send(packet: SendablePacket) {
        channel.writeAndFlush(packet)
    }


    suspend fun connect() {
        if (binding || isStopped) return
        binding = true
        bootstrap.connect(socketAddress).awaitSuspending().apply {
            if (isSuccess) {
                isConnected = true
                isStopped = true
                channel = channel()
            }
            binding = false
            callEvent(ClientConnectEvent(isSuccess, channel))
        }
    }

    suspend fun disconnect() {
        if (!isConnected || !isStopped) return
        try {
            eventLoopGroup.shutdownGracefully()
        }finally {
            channel?.closeFuture()?.awaitSuspending()
            isConnected = false
            callEvent(ServerClosedEvent())
            coroutineScope.cancel("Client stopped")
        }
    }
}