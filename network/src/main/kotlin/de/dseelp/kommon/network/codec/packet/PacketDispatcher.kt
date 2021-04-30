package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.event.EventDispatcher
import de.dseelp.kommon.network.codec.ConnectionState
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
    private val _packetFlow = MutableSharedFlow<Pair<ChannelHandlerContext, ReceivablePacket>>(extraBufferCapacity = 500000, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val packetFlow = _packetFlow.asSharedFlow()
    private val _eventFlow = MutableSharedFlow<Any>()
    val eventFlow = _eventFlow.asSharedFlow()

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

        suspend inline fun <reified R: ReceivablePacket> Channel.sendPacket(packet: SendablePacket, timeout: Long = 5000): Deferred<R> = coroutineScope {
            return@coroutineScope async {
                return@async withTimeout(timeout) {
                    val id = dispatcher.getIdForSendablePacket(packet)
                    writeAndFlush(packet)
                    dispatcher.packetFlow.filterIsInstance<Pair<ChannelHandlerContext, ReceivablePacket>>().filter {
                        val receivedId = it.second.responseId
                        if (it.first.channel().id() != id()) return@filter false
                        if (receivedId == null) false
                        else id == receivedId
                    }.first().second as R
                }

            }
        }
    }

    fun getIdForSendablePacket(packet: SendablePacket): UUID {
        if (sendedPacketIdCache.containsKey(packet)) return sendedPacketIdCache[packet]!!
        val id = UUID.randomUUID()
        sendedPacketIdCache[packet] = id
        return id
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
        _packetFlow.emit(ctx to packet)
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

    suspend inline fun <reified T : ReceivablePacket> on(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        sequential: Boolean = true,
        crossinline block: suspend PacketDispatcherDslScope.(ctx: ChannelHandlerContext, packet: T) -> Unit
    ) = coroutineScope.launch(dispatcher) {
        packetFlow.filterIsInstance<Pair<ChannelHandlerContext, T>>().collect {
            if (sequential) block(dslScope, it.first, it.second)
            else launch {
                block(dslScope, it.first, it.second)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend inline fun <reified T : ReceivablePacket> addHandler(
        function: KFunction<*>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        sequential: Boolean = false
    ) = coroutineScope.launch(dispatcher) {
        val parameters = function.parameters
        val ctxType = typeOf<ChannelHandlerContext>()
        val packetType = typeOf<T>()
        val ctxParameter = parameters.firstOrNull { it.type == ctxType }
            ?: throw IllegalArgumentException("Can't find ChannelHandlerContext parameter in function")
        val packetParameter = parameters.firstOrNull { it.type == packetType }
            ?: throw IllegalArgumentException("Can't find T (ReceivablePacket) parameter in function")
        if (parameters.filter { it != ctxParameter && it != packetParameter }
                .firstOrNull { !it.isOptional } == null) throw IllegalArgumentException("The only non-optional parameters on the function must be the ChannelHandlerContext and T (ReceivablePacket) parameter")

        packetFlow.filterIsInstance<Pair<ChannelHandlerContext, T>>().collect {
            if (sequential) function.callBy(mapOf(ctxParameter to it.first, packetParameter to it.second))
            else launch {
                function.callBy(mapOf(ctxParameter to it.first, packetParameter to it.second))
            }
        }
    }

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
        packets[state]?.get(packetIdentifier)

    class PacketHandlerScope : CoroutineScope {
        private val job = Job()

        override val coroutineContext: CoroutineContext
            get() = job
    }
}

suspend fun PacketDispatcher.PacketDispatcherDslScope.test(block: suspend PacketDispatcher.PacketDispatcherDslScope.() -> Unit) {
    block.invoke(this)
}