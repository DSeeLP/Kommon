package de.dseelp.kommon.network.codec

import io.netty.channel.Channel

data class UnknownPacketReceivedEvent(val packetIdentifier: Int, val state: ConnectionState, val channel: Channel)