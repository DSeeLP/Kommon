package de.dseelp.kommon.network.utils

import de.dseelp.kommon.network.codec.ConnectionState

abstract class ConnectionInfo {
    abstract val state: ConnectionState

    abstract fun changeState(state: ConnectionState): ConnectionInfo
}

data class DefaultConnectionInfo(override val state: ConnectionState = ConnectionState.DEFAULT): ConnectionInfo() {
    override fun changeState(state: ConnectionState): ConnectionInfo = copy(state = state)
}