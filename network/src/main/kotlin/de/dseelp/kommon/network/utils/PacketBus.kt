package de.dseelp.kommon.network.utils

import de.dseelp.kommon.network.codec.packet.PacketDispatcher
import de.dseelp.kommon.network.codec.packet.ReceivablePacket
import io.netty.channel.ChannelHandlerContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class PacketBus {
    private val handlers = mutableListOf<Handler>()

    suspend fun call(scope: PacketDispatcher.PacketDispatcherDslScope, ctx: ChannelHandlerContext, packet: ReceivablePacket) {
        val packetClass = packet::class
        for (handler in handlers) {
            if (handler.clazz == packetClass || packetClass.isSubclassOf(handler.clazz)) handler.invoke(scope, ctx, packet)
        }
    }


    inline fun <reified T: ReceivablePacket> addHandler(function: KFunction<*>) = addHandler(Handler.MethodHandler(T::class, function))

    fun addHandler(handler: Handler) {
        handlers.add(handler)
    }

    inline fun <reified T: ReceivablePacket> addHandler(noinline block: suspend PacketDispatcher.PacketDispatcherDslScope.(ctx: ChannelHandlerContext, packet: T) -> Unit) = addHandler(
        Handler.LambdaHandler(T::class, block))

    sealed class Handler {
        abstract suspend fun invoke(
            scope: PacketDispatcher.PacketDispatcherDslScope,
            context: ChannelHandlerContext,
            packet: ReceivablePacket
        )

        abstract val clazz: KClass<out ReceivablePacket>

        class LambdaHandler<T>(
            override val clazz: KClass<out ReceivablePacket>,
            val lambda: suspend PacketDispatcher.PacketDispatcherDslScope.(ctx: ChannelHandlerContext, packet: T) -> Unit
        ) : Handler() {
            override suspend fun invoke(
                scope: PacketDispatcher.PacketDispatcherDslScope,
                context: ChannelHandlerContext,
                packet: ReceivablePacket
            ) {
                @Suppress("UNCHECKED_CAST")
                lambda.invoke(scope, context, packet as T)
            }
        }

        class MethodHandler(override val clazz: KClass<out ReceivablePacket>, val function: KFunction<*>) : Handler() {

            private val parameters = function.parameters
            val ctxParameter = parameters.firstOrNull { it.type.jvmErasure == ChannelHandlerContext::class }
                ?: throw IllegalArgumentException("Can't find ChannelHandlerContext parameter in function")
            val packetParameter = parameters.firstOrNull { it.type.jvmErasure == clazz }
                ?: throw IllegalArgumentException("Can't find ReceivablePacket parameter in function")
            val extensionParameter = function.extensionReceiverParameter

            init {

                if (parameters.filter { it != ctxParameter && it != packetParameter }
                        .firstOrNull { !it.isOptional } == null) throw IllegalArgumentException("The only non-optional parameters on the function must be the ChannelHandlerContext and ReceivablePacket parameter")
            }

            override suspend fun invoke(
                scope: PacketDispatcher.PacketDispatcherDslScope,
                context: ChannelHandlerContext,
                packet: ReceivablePacket
            ) {
                val parameterMap = mapOf(
                    ctxParameter to context,
                    packetParameter to packet
                )
                val finalParams =
                    if (extensionParameter == null) parameterMap else parameterMap + mapOf(extensionParameter to scope)
                if (function.isSuspend) function.callSuspendBy(finalParams)
                else function.callBy(finalParams)
            }
        }
    }
}