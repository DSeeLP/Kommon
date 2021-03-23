package de.dseelp.kommon.event

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.CoroutineContext

class EventBus(private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = defaultDispatcher+job
    private val mutableSharedFlow= MutableSharedFlow<Any>(replay=0)

    val eventFlow = mutableSharedFlow.asSharedFlow()

    suspend fun <T> emit(event: T) {
        if (!isActive) return
        if (event == null) return
        mutableSharedFlow.emit(event)
    }
}