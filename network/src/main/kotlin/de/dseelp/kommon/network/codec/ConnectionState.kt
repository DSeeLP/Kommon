package de.dseelp.kommon.network.codec

class ConnectionState(val id: Int, val name: String) {
    companion object {
        val DEFAULT = ConnectionState(1, "DEFAULT")
    }
}