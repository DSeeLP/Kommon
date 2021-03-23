package de.dseelp.kommon.network.utils

import de.dseelp.kommon.network.codec.ConnectionState
import io.netty.channel.*
import kotlinx.coroutines.*
import kotlinx.serialization.InternalSerializationApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resumeWithException
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf

val Channel.scope: ChannelScope
    get() = getScope(id())

val ChannelHandlerContext.scope: ChannelScope
    get() = getScope(channel().id())

var Channel.internalInfo: ConnectionInfo
    get() = getChannelData(id()).second
    set(value) {
        channelMap[id()] = getChannelData(id()).first to value
    }

fun Int.toHex(): String = "0x"+"%02x".format(this)

@OptIn(InternalNetworkApi::class)
inline fun <reified T: ConnectionInfo> customInfo(): ReadWriteProperty<Channel, T> {
    if (!T::class.isData) throw UnsupportedOperationException("The ConnectionInfo must be a data class!")
    return CustomInfo()
}

@InternalNetworkApi
class CustomInfo<T: ConnectionInfo>: ReadWriteProperty<Channel, T> {
    override fun setValue(thisRef: Channel, property: KProperty<*>, value: T) {
        thisRef.internalInfo = value
    }

    override fun getValue(thisRef: Channel, property: KProperty<*>): T = thisRef.internalInfo as T
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class InternalNetworkApi()


suspend fun ChannelFuture.awaitSuspending() = suspendCancellableCoroutine<ChannelFuture> { continuation ->
    val listener = ChannelFutureListener {
        continuation.resumeWith(Result.success(it))
    }
    addListener(listener)
    continuation.invokeOnCancellation {
        if (isCancellable && !isCancelled) {
            removeListener(listener)
            try {
                this.cancel(true)
            }catch (ex: Exception) {
                continuation.resumeWithException(ex)
            }
        }
    }
}


private val channelMap = mutableMapOf<ChannelId, Pair<ChannelScope, ConnectionInfo>>()

private fun getScope(id: ChannelId): ChannelScope = getChannelData(id).first

private fun getChannelData(id: ChannelId): Pair<ChannelScope, ConnectionInfo> = channelMap.getOrPut(id) {
    ChannelScope(id) to DefaultConnectionInfo()
}

class ChannelScope(val id: ChannelId): CoroutineScope {
    private val job = Job()
    private val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        GlobalScope.launch {
            if (!(findRootCause(throwable) is CancellationException)) {

            }
        }
    }

    fun findRootCause(throwable: Throwable): Throwable {
        var root: Throwable? = throwable
        while (root?.cause != null && root.cause !== root) {
            root = root.cause
        }
        return root!!
    }
    override val coroutineContext: CoroutineContext
        get() = job + exceptionHandler + Dispatchers.IO

    fun closeScope() {
        try {
            job.cancel()
            channelMap.remove(id)
        }catch (ex: CancellationException) {}
    }
}

object ChannelExtensions {
    fun resetChannel(id: ChannelId) {
        if (channelMap.containsKey(id)) {
            channelMap[id]?.first?.closeScope()
        }
    }
}