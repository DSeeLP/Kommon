package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.utils.InternalNetworkApi
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

interface Packet {
    val packetIdentifier: Int

    val readOnly: Boolean

    fun boolean(default: Boolean? = null) = SimplePacketData(
        readOnly,
        { it.readBoolean() },
        { buffer, value -> buffer.writeBoolean(value) },
        default
    )

    fun byte(default: Int? = null) = SimplePacketData(readOnly, {it.readByte().toInt()}, { buffer, value -> buffer.writeByte(value)}, default)
    @ExperimentalUnsignedTypes
    fun unsignedByte(default: Int? = null) = SimplePacketData(readOnly, {it.readUnsignedByte()}, { buffer, value -> buffer.writeByte(value.toInt())}, default)
    fun short(default: Short? = null) = SimplePacketData(readOnly, {it.readShort()}, { buffer, value -> buffer.writeShort(value.toInt())}, default)
    @ExperimentalUnsignedTypes
    fun unsignedShort(default: UShort? = null) = SimplePacketData(readOnly, {it.readUnsignedShort().toUShort()}, { buffer, value -> buffer.writeShort(value.toInt())}, default)
    fun int(default: Int? = null) = SimplePacketData(readOnly, {it.readInt()}, { buffer, value -> buffer.writeInt(value)}, default)
    fun long(default: Long? = null) = SimplePacketData(readOnly, {it.readLong()}, { buffer, value -> buffer.writeLong(value)}, default)
    fun float(default: Float? = null) = SimplePacketData(readOnly, {it.readFloat()}, { buffer, value -> buffer.writeFloat(value)}, default)
    fun double(default: Double? = null) = SimplePacketData(readOnly, {it.readDouble()}, { buffer, value -> buffer.writeDouble(value)}, default)
    fun string(default: String? = null) = SimplePacketData(readOnly, {BufferUtils.readString(Int.MAX_VALUE, it)}, { buffer, value -> BufferUtils.writeString(value, buffer)}, default)
    fun chat(default: String? = null) = SimplePacketData(readOnly, {BufferUtils.readString(131071, it)}, { buffer, value -> BufferUtils.writeString(value, buffer)})
    fun identifier(default: String? = null) = SimplePacketData(readOnly, {BufferUtils.readString(131071, it)}, { buffer, value -> BufferUtils.writeString(value, buffer)})
    fun varInt(default: Int? = null) = SimplePacketData(readOnly, {BufferUtils.readVarInt(it)}, { buffer, value -> BufferUtils.writeVarInt(value, buffer)}, default)
    fun varLong(default: Long? = null) = SimplePacketData(readOnly, {BufferUtils.readVarLong(it)}, { buffer, value -> BufferUtils.writeVarLong(value, buffer)}, default)
    fun angle(default: Int? = null) = SimplePacketData(readOnly, {it.readInt()}, { buffer, value -> BufferUtils.writeInt(value, buffer)}, default)
    fun uuid(default: UUID? = null) = SimplePacketData(readOnly, {BufferUtils.readUniqueId(it)}, { buffer, value -> BufferUtils.writeUniqueId(value, buffer)}, default)
    fun <T> optional(data: PacketData<T, out T?>, condition: () -> Boolean): OptionalPacketData<T> = OptionalPacketData(readOnly, data, condition)
    fun byteArray(default: ByteArray? = null) = SimplePacketData(readOnly, {BufferUtils.readByteArray(Int.MAX_VALUE, it)}, { buffer, value -> BufferUtils.writeByteArray(value, buffer)}, default)
}