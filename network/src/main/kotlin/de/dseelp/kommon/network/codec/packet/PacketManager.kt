package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.codec.ConnectionState
import java.lang.Exception
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object PacketManager {
    val packets = hashMapOf<ConnectionState, HashMap<Int, KClass<out ReceivablePacket>>>()

    fun registerPacket(clazz: KClass<out ReceivablePacket>) {
        val packet = try {
            clazz.createInstance()
        }catch (e: Exception) {
            throw IllegalStateException("A receivable packet class must have a empty constructor")
        }
        val packetMap = packets[packet.state]
        if (packetMap == null) {
            packets[packet.state] = hashMapOf(packet.packetIdentifier to clazz)
            return
        }
        packetMap[packet.packetIdentifier] = clazz
    }

    fun getPacketClass(state: ConnectionState, packetIdentifier: Int): KClass<out ReceivablePacket>? = packets[state]?.get(packetIdentifier)
}