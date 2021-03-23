package de.dseelp.kommon.network.codec

class ConnectionState(val id: Int, val name: String) {
    companion object {
        val ALL = ConnectionState(0, "ALL")
        val DEFAULT = ConnectionState(1, "DEFAULT")
    }
}