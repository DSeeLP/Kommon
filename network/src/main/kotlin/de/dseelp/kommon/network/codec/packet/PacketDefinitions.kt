package de.dseelp.kommon.network.codec.packet

import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets
import kotlin.reflect.KProperty
import java.util.UUID
import kotlin.experimental.and
import kotlin.experimental.or


object BufferUtils {
    fun readBytes(buffer: ByteBuf): ByteArray {
        val readableBytes = buffer.readableBytes()
        if (readableBytes == 0) return ByteArray(0)
        val array = ByteArray(readableBytes)
        buffer.readBytes(array, 0, readableBytes)
        return array
    }

    fun readVarInt(buffer: ByteBuf): Int {
        var numRead = 0
        var result = 0
        var read: Byte
        do {
            read = buffer.readByte()
            val value = (read and 127).toInt()
            result = result or (value shl 7 * numRead)
            numRead++
            if (numRead > 5) {
                throw RuntimeException("VarInt is too big")
            }
        } while (read and 128.toByte() != 0.toByte())
        return result
    }

    fun readVarLong(buffer: ByteBuf): Long {
        var numRead = 0
        var result: Long = 0
        var read: Byte
        do {
            read = buffer.readByte()
            val value = (read and 127).toLong()
            result = result or (value shl 7 * numRead)
            numRead++
            if (numRead > 10) {
                throw RuntimeException("VarLong is too big")
            }
        } while (read and 128.toByte() != 0.toByte())
        return result
    }

    fun readString(maxLength: Int, buffer: ByteBuf): String {
        val length = readVarInt(buffer)
        if (length > maxLength) throw IllegalStateException("String length ($length) was higher than the max length of $maxLength")
        val bytes: ByteArray = readBytes(length, buffer)
        return String(bytes)
    }

    fun readBytes(length: Int, buffer: ByteBuf): ByteArray {
        val buf = buffer.readBytes(length)
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        buf.release()
        return bytes
    }

    fun readUniqueId(buffer: ByteBuf): UUID {
        val most = buffer.readLong()
        val least = buffer.readLong()
        return UUID(most, least)
    }

    fun readSizedStringArray(maxLength: Int, buffer: ByteBuf): Array<String> {
        val size = readVarInt(buffer)
        val strings = arrayOfNulls<String>(size)
        for (i in 0 until size) {
            strings[i] = readString(maxLength, buffer)
        }
        return strings.filterNotNull().toTypedArray()
    }

    fun readVarIntArray(buffer: ByteBuf): IntArray {
        val size = readVarInt(buffer)
        val array = IntArray(size)
        for (i in 0 until size) {
            array[i] = readVarInt(buffer)
        }
        return array
    }

    fun getRemainingBytes(buffer: ByteBuf): ByteArray {
        return readBytes(buffer.readableBytes(), buffer)
    }

    fun readByteArray(maxLength: Int, buffer: ByteBuf): ByteArray {
        val length = readVarInt(buffer)
        if (length > maxLength) throw IllegalStateException("String length ($length) was higher than the max length of $maxLength")
        return readBytes(length, buffer)
    }
    fun writeBoolean(b: Boolean, buffer: ByteBuf) {
        buffer.writeBoolean(b)
    }

    fun writeByte(b: Byte, buffer: ByteBuf) {
        buffer.writeByte(b.toInt())
    }

    @ExperimentalUnsignedTypes
    fun writeUnsignedByte(b: UByte, buffer: ByteBuf) {
        buffer.writeByte(b.toInt())
    }

    fun writeChar(c: Char, buffer: ByteBuf) {
        buffer.writeChar(c.toInt())
    }

    fun writeShort(s: Short, buffer: ByteBuf) {
        buffer.writeShort(s.toInt())
    }

    @ExperimentalUnsignedTypes
    fun writeUnsignedShort(s: UShort, buffer: ByteBuf) {
        buffer.writeShort(s.toInt())
    }

    fun writeInt(i: Int, buffer: ByteBuf) {
        buffer.writeInt(i)
    }

    fun writeLong(l: Long, buffer: ByteBuf) {
        buffer.writeLong(l)
    }

    fun writeFloat(f: Float, buffer: ByteBuf) {
        buffer.writeFloat(f)
    }

    fun writeDouble(d: Double, buffer: ByteBuf) {
        buffer.writeDouble(d)
    }

    fun writeVarInt(intValue: Int, buffer: ByteBuf) {
        var value = intValue
        do {
            var temp = (value and 127).toByte()
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value = value ushr 7
            if (value != 0) {
                temp = temp or 128.toByte()
            }
            buffer.writeByte(temp.toInt())
        } while (value != 0)
    }

    fun writeVarLong(longValue: Long, buffer: ByteBuf) {
        var value = longValue
        do {
            var temp = (value and 127).toByte()
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value = value ushr 7
            if (value != 0L) {
                temp = temp or 128.toByte()
            }
            buffer.writeInt(temp.toInt())
        } while (value != 0L)
    }

    fun writeString(string: String, buffer: ByteBuf) {
        val bytes = string.toByteArray(StandardCharsets.UTF_8)
        writeVarInt(bytes.size, buffer)
        writeBytes(bytes, buffer)
    }

    fun writeByteArray(bytes: ByteArray, buffer: ByteBuf) {
        writeVarInt(bytes.size, buffer)
        writeBytes(bytes, buffer)
    }

    fun writeVarIntArray(array: IntArray?, buffer: ByteBuf) {
        if (array == null) {
            writeVarInt(0, buffer)
            return
        }
        writeVarInt(array.size, buffer)
        for (element in array) {
            writeVarInt(element, buffer)
        }
    }

    fun writeBytes(bytes: ByteArray?, buffer: ByteBuf) {
        buffer.writeBytes(bytes)
    }

    fun writeStringArray(array: Array<String>, buffer: ByteBuf) {
        writeVarInt(array.size, buffer)
        for (element in array) {
            writeString(element, buffer)
        }
    }

    fun writeUniqueId(uniqueId: UUID, buffer: ByteBuf) {
        writeLong(uniqueId.mostSignificantBits, buffer)
        writeLong(uniqueId.leastSignificantBits, buffer)
    }

    fun toByteArray(buffer: ByteBuf): ByteArray {
        val bytes = ByteArray(buffer.readableBytes())
        val readerIndex = buffer.readerIndex()
        buffer.getBytes(readerIndex, bytes)
        return bytes
    }
}

class SimplePacketData<T>(private val readOnly: Boolean, private val read: (buffer: ByteBuf)->T, private val write: (buffer: ByteBuf, value: T)->Unit, defaultValue: T? = null): PacketData<T, T>() {
    var value: T? = defaultValue
        private set

    override fun read(buffer: ByteBuf) {
        value = read.invoke(buffer)
    }

    override fun write(buffer: ByteBuf) {
        if (readOnly) return
        value?.let { write.invoke(buffer, it) }
    }

    override operator fun getValue(thisRef: Any, property: KProperty<*>): T = value!!

    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

    override fun getValue(): T = value!!

    override fun setValue(value: T) {
        this.value = value
    }
}

class OptionalPacketData<V>(private val readOnly: Boolean, private val data: PacketData<V, out V?>, private val condition: () -> Boolean): PacketData<V, V?>() {

    var match = false

    override fun read(buffer: ByteBuf) {
        match = condition.invoke()
        if (match) {
            data.read(buffer)
        }
    }

    override fun write(buffer: ByteBuf) {
        if (readOnly) return
        data.write(buffer)
    }

    override fun getValue(): V? {
        if (!match) return null
        return data.getValue()
    }

    override fun setValue(value: V) {
        if (readOnly) return
        data.setValue(value)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: V) = setValue(value)

    override fun getValue(thisRef: Any, property: KProperty<*>): V? = getValue()
}

sealed class PacketData<T, V> {
    abstract fun read(buffer: ByteBuf)
    abstract fun write(buffer: ByteBuf)
    internal abstract fun getValue(): V
    internal abstract fun setValue(value: T)
    abstract operator fun getValue(thisRef: Any, property: KProperty<*>): V
    abstract operator fun setValue(thisRef: Any, property: KProperty<*>, value: T)
}