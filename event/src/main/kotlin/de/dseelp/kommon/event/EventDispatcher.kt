package de.dseelp.kommon.event

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventDispatcher(val eventBus: EventBus = EventBus()) {
    suspend fun call(event: Any, async: Boolean = false) = coroutineScope {
        if (async) eventBus.launch {
            eventBus.emit(event)
        }
        else eventBus.emit(event)

    }

    suspend inline fun <reified T> on(dispatcher: CoroutineDispatcher? = null, sequential: Boolean = false, crossinline block: suspend (event: T) -> Unit) = coroutineScope {
        if (dispatcher == null) {
            eventBus.launch {
                eventBus.eventFlow.filterIsInstance<T>().collect {
                    if (sequential) block(it)
                    else launch {
                        block(it)
                    }
                }
            }
        }else {
            eventBus.launch(dispatcher) {
                eventBus.eventFlow.filterIsInstance<T>().collect {
                    if (sequential) block(it)
                    else launch {
                        block(it)
                    }
                }
            }
        }
    }

    /*suspend inline fun <reified T> wait(): T {
        eventBus.eventFlow.first()
        eventBus.eventFlow.filterIsInstance<T>().single()
    }*/
}