package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.codec.ConnectionState
import de.dseelp.kommon.network.utils.PacketBus
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.typeOf

open class PacketDispatcher(val isResponseEnabled: Boolean = true) {
    private val _eventFlow = MutableSharedFlow<Any>()
    val eventFlow = _eventFlow.asSharedFlow()

    val packetBus = PacketBus()

    var maxCacheCapacity = 200

    val receivedPacketIdCache = object : LinkedHashMap<ReceivablePacket, Pair<UUID, UUID?>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ReceivablePacket, Pair<UUID, UUID?>>?): Boolean {
            return size > maxCacheCapacity
        }
    }

    val sendedPacketIdCache = object : LinkedHashMap<SendablePacket, UUID>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SendablePacket, UUID>?): Boolean {
            return size > maxCacheCapacity
        }
    }
    val channelCache = object : LinkedHashMap<UUID, Channel>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, Channel>?): Boolean {
            return size > maxCacheCapacity
        }
    }

    val coroutineScope = PacketHandlerScope()

    val dslScope = PacketDispatcherDslScope(this)

    val waitingPackets = hashMapOf<UUID, CompletableDeferred<*>>()/*object : LinkedHashMap<UUID, CompletableDeferred<*>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, CompletableDeferred<*>>?): Boolean {
            return size > maxCacheCapacity
        }
    }*/

    init {
        packetBus.addHandler<ReceivablePacket> { ctx, packet ->
            val p = packet
            p.responseId
            val waitingPackets = waitingPackets
            val deferred = waitingPackets[packet.responseId] ?: return@addHandler
            @Suppress("UNCHECKED_CAST")
            deferred as CompletableDeferred<ReceivablePacket>
            deferred.complete(packet)
        }
    }

    class PacketDispatcherDslScope internal constructor(val dispatcher: PacketDispatcher) {
        val ReceivablePacket.id: UUID
            get() = dispatcher.receivedPacketIdCache[this]!!.first

        val ReceivablePacket.isResponse
            get() = dispatcher.receivedPacketIdCache[this]!!.second != null

        val ReceivablePacket.responseId
            get() = dispatcher.receivedPacketIdCache[this]!!.second

        fun ReceivablePacket.respond(packet: SendablePacket) {
            dispatcher.channelCache[id]!!.writeAndFlush(id to packet)
        }

        inline fun <reified R: ReceivablePacket> Channel.sendPacket(packet: SendablePacket): Deferred<R> {
            val id = dispatcher.getIdForSendablePacket(packet)
            val deferred = CompletableDeferred<R>()
            dispatcher.waitingPackets[id] = deferred
            writeAndFlush(packet)
            return deferred
        }
    }

    fun getIdForSendablePacket(packet: SendablePacket): UUID {
        if (sendedPacketIdCache.containsKey(packet)) return sendedPacketIdCache[packet]!!
        val id = UUID.randomUUID()
        sendedPacketIdCache[packet] = id
        return id
    }

    suspend inline fun <reified R: Any> waitForEvent(timeout: Long = 5000): Deferred<R> = coroutineScope {
        return@coroutineScope async {
            return@async withTimeout(timeout) {
                eventFlow.filterIsInstance<R>().first()
            }
        }
    }


    suspend inline fun <reified T : Any> onEvent(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        sequential: Boolean = false,
        crossinline block: suspend PacketDispatcherDslScope.(event: T) -> Unit
    ) = coroutineScope.launch(dispatcher) {
        eventFlow.filterIsInstance<T>().collect {
            if (sequential) block(dslScope, it)
            else launch {
                block(dslScope, it)
            }
        }
    }

    suspend fun call(ctx: ChannelHandlerContext, packet: ReceivablePacket, messageId: UUID, responseId: UUID? = null) {
        receivedPacketIdCache[packet] = messageId to responseId
        channelCache[messageId] = ctx.channel()
        if (receivedPacketIdCache.size > 2000) {
            receivedPacketIdCache
        }
        //_packetFlow.emit(ctx to packet)
        packetBus.call(dslScope, ctx, packet)
        //receivedPacketIdCache.remove(packet)
        //channelCache.remove(messageId)
        /*coroutineScope {
            launch(coroutineContext) {
                _packetFlow.emit(ctx to packet)
            }
        }*/
    }

    suspend fun callEvent(event: Any, async: Boolean = false): Unit = coroutineScope {
        if (async) launch(coroutineContext) {
            _eventFlow.emit(event)
        }
        else _eventFlow.emit(event)
    }

    inline fun <reified T : ReceivablePacket> on(
        noinline block: suspend PacketDispatcherDslScope.(ctx: ChannelHandlerContext, packet: T) -> Unit
    ) = packetBus.addHandler(block)

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : ReceivablePacket> addHandler(
        function: KFunction<*>,
    ) = packetBus.addHandler<T>(function)

    val packets = hashMapOf<ConnectionState, HashMap<Int, KClass<out ReceivablePacket>>>()

    inline fun <reified T : ReceivablePacket> registerPacket() = registerPacket(T::class)

    fun registerPacket(clazz: KClass<out ReceivablePacket>) {
        val packet = try {
            clazz.createInstance()
        } catch (e: Exception) {
            throw IllegalStateException("A receivable packet class must have a empty constructor")
        }
        val packetMap = packets[packet.state]
        if (packetMap == null) {
            packets[packet.state] = hashMapOf(packet.packetIdentifier to clazz)
            return
        }
        packetMap[packet.packetIdentifier] = clazz

    }

    fun getPacketClass(state: ConnectionState, packetIdentifier: Int): KClass<out ReceivablePacket>? =
        packets[state]?.get(packetIdentifier) ?: packets[ConnectionState.ANY]?.get(packetIdentifier)

    class PacketHandlerScope : CoroutineScope {
        private val job = Job()

        override val coroutineContext: CoroutineContext
            get() = job
    }
}

suspend operator fun <R> PacketDispatcher.PacketDispatcherDslScope.invoke(block: suspend PacketDispatcher.PacketDispatcherDslScope.() -> R): R = block.invoke(this)