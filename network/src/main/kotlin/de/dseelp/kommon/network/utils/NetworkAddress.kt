package de.dseelp.kommon.network.utils

sealed class NetworkAddress {
    companion object {
        inline fun inet(host: String, port: Int) = InetNetworkAddress(host, port)
        inline fun local(id: String) = LocalNetworkAddress(id)
        val ANY = LocalNetworkAddress.ANY
    }

    data class InetNetworkAddress(val host: String, val port: Int): NetworkAddress()
    data class LocalNetworkAddress(val id: String): NetworkAddress() {
        companion object {
            val ANY = LocalNetworkAddress("ANY")
        }
    }
}