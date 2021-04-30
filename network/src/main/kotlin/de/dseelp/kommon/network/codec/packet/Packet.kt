package de.dseelp.kommon.network.codec.packet

import de.dseelp.kommon.network.codec.packet.BufferUtils.readByteArray
import de.dseelp.kommon.network.codec.packet.BufferUtils.readSizedString
import de.dseelp.kommon.network.codec.packet.BufferUtils.readUniqueId
import de.dseelp.kommon.network.codec.packet.BufferUtils.readVarInt
import de.dseelp.kommon.network.codec.packet.BufferUtils.readVarLong
import de.dseelp.kommon.network.codec.packet.BufferUtils.writeByteArray
import de.dseelp.kommon.network.codec.packet.BufferUtils.writeSizedString
import de.dseelp.kommon.network.codec.packet.BufferUtils.writeUniqueId
import de.dseelp.kommon.network.codec.packet.BufferUtils.writeVarInt
import de.dseelp.kommon.network.codec.packet.BufferUtils.writeVarLong
import java.util.*

interface Packet {
    val packetIdentifier: Int

    val readOnly: Boolean

    fun boolean(default: Boolean? = null) = SimplePacketData(
        readOnly,
        { it.readBoolean() },
        { buffer, value -> buffer.writeBoolean(value) },
        default
    )

    fun byte(default: Int? = null) =
        SimplePacketData(readOnly, { it.readByte().toInt() }, { buffer, value -> buffer.writeByte(value) }, default)

    @ExperimentalUnsignedTypes
    fun unsignedByte(default: Int? = null) = SimplePacketData(
        readOnly,
        { it.readUnsignedByte() },
        { buffer, value -> buffer.writeByte(value.toInt()) },
        default
    )

    fun short(default: Short? = null) =
        SimplePacketData(readOnly, { it.readShort() }, { buffer, value -> buffer.writeShort(value.toInt()) }, default)

    @ExperimentalUnsignedTypes
    fun unsignedShort(default: UShort? = null) = SimplePacketData(
        readOnly,
        { it.readUnsignedShort().toUShort() },
        { buffer, value -> buffer.writeShort(value.toInt()) },
        default
    )

    fun int(default: Int? = null) =
        SimplePacketData(readOnly, { it.readInt() }, { buffer, value -> buffer.writeInt(value) }, default)

    fun long(default: Long? = null) =
        SimplePacketData(readOnly, { it.readLong() }, { buffer, value -> buffer.writeLong(value) }, default)

    fun float(default: Float? = null) =
        SimplePacketData(readOnly, { it.readFloat() }, { buffer, value -> buffer.writeFloat(value) }, default)

    fun double(default: Double? = null) =
        SimplePacketData(readOnly, { it.readDouble() }, { buffer, value -> buffer.writeDouble(value) }, default)

    fun string(default: String? = null) = SimplePacketData(
        readOnly,
        { it.readSizedString(Int.MAX_VALUE) },
        { buffer, value -> buffer.writeSizedString(value) },
        default
    )

    fun chat(default: String? = null) =
        SimplePacketData(readOnly, { it.readSizedString(131071) }, { buffer, value -> buffer.writeSizedString(value) })

    fun identifier(default: String? = null) =
        SimplePacketData(readOnly, { it.readSizedString(131071) }, { buffer, value -> buffer.writeSizedString(value) })

    fun varInt(default: Int? = null) =
        SimplePacketData(readOnly, { it.readVarInt() }, { buffer, value -> buffer.writeVarInt(value) }, default)

    fun varLong(default: Long? = null) =
        SimplePacketData(readOnly, { it.readVarLong() }, { buffer, value -> buffer.writeVarLong(value) }, default)

    fun angle(default: Int? = null) =
        SimplePacketData(readOnly, { it.readInt() }, { buffer, value -> buffer.writeInt(value) }, default)

    fun uuid(default: UUID? = null) =
        SimplePacketData(readOnly, { it.readUniqueId() }, { buffer, value -> buffer.writeUniqueId(value) }, default)

    fun <T> optional(data: PacketData<T, out T?>, condition: () -> Boolean): OptionalPacketData<T> =
        OptionalPacketData(readOnly, data, condition)

    fun byteArray(default: ByteArray? = null) = SimplePacketData(
        readOnly,
        { it.readByteArray(Int.MAX_VALUE) },
        { buffer, value -> buffer.writeByteArray(value) },
        default
    )
}