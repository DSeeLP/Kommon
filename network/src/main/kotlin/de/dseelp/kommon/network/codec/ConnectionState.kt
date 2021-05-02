package de.dseelp.kommon.network.codec

class ConnectionState(val id: Int, val name: String) {
    companion object {
        val ANY = ConnectionState(-1, "ANY")
        val DEFAULT = ConnectionState(1, "DEFAULT")
    }
}