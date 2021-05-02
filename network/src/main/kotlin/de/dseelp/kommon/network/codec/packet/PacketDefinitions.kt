package de.dseelp.kommon.network.codec.packet

import io.netty.buffer.ByteBuf
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.reflect.KProperty


object BufferUtils {
    fun readBytesUtils(buffer: ByteBuf): ByteArray {
        val readableBytes = buffer.readableBytes()
        if (readableBytes == 0) return ByteArray(0)
        val array = ByteArray(readableBytes)
        buffer.readBytes(array, 0, readableBytes)
        return array
    }

    fun ByteBuf.readVarInt(): Int {
        var numRead = 0
        var result = 0
        var read: Byte
        do {
            read = readByte()
            val value = (read and 127).toInt()
            result = result or (value shl 7 * numRead)
            numRead++
            if (numRead > 5) {
                throw RuntimeException("VarInt is too big")
            }
        } while (read and 128.toByte() != 0.toByte())
        return result
    }

    fun ByteBuf.readVarLong(): Long {
        var numRead = 0
        var result: Long = 0
        var read: Byte
        do {
            read = readByte()
            val value = (read and 127).toLong()
            result = result or (value shl 7 * numRead)
            numRead++
            if (numRead > 10) {
                throw RuntimeException("VarLong is too big")
            }
        } while (read and 128.toByte() != 0.toByte())
        return result
    }

    fun ByteBuf.readSizedString(maxLength: Int): String {
        val length = readVarInt()
        if (length > maxLength) throw IllegalStateException("String length ($length) was higher than the max length of $maxLength")
        val bytes: ByteArray = readBytesUtils(length)
        return String(bytes)
    }

    fun ByteBuf.readBytesUtils(length: Int): ByteArray {
        val buf = readBytes(length)
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        buf.release()
        return bytes
    }

    fun ByteBuf.readUniqueId(): UUID {
        val most = readLong()
        val least = readLong()
        return UUID(most, least)
    }

    fun ByteBuf.readSizedStringArray(maxLength: Int): Array<String> {
        val size = readVarInt()
        val strings = arrayOfNulls<String>(size)
        for (i in 0 until size) {
            strings[i] = readSizedString(maxLength)
        }
        return strings.filterNotNull().toTypedArray()
    }

    fun ByteBuf.readVarIntArray(): IntArray {
        val size = readVarInt()
        val array = IntArray(size)
        for (i in 0 until size) {
            array[i] = readVarInt()
        }
        return array
    }

    fun ByteBuf.getRemainingBytes(): ByteArray {
        return readBytesUtils(readableBytes())
    }

    fun ByteBuf.readByteArray(maxLength: Int): ByteArray {
        val length = readVarInt()
        if (length > maxLength) throw IllegalStateException("String length ($length) was higher than the max length of $maxLength")
        return readBytesUtils(length)
    }

    fun ByteBuf.writeVarInt(intValue: Int) {
        var value = intValue
        do {
            var temp = (value and 127).toByte()
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value = value ushr 7
            if (value != 0) {
                temp = temp or 128.toByte()
            }
            writeByte(temp.toInt())
        } while (value != 0)
    }

    fun ByteBuf.writeVarLong(longValue: Long) {
        var value = longValue
        do {
            var temp = (value and 127).toByte()
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value = value ushr 7
            if (value != 0L) {
                temp = temp or 128.toByte()
            }
            writeInt(temp.toInt())
        } while (value != 0L)
    }

    fun ByteBuf.writeSizedString(string: String) {
        val bytes = string.toByteArray(StandardCharsets.UTF_8)
        writeVarInt(bytes.size)
        writeBytes(bytes)
    }

    fun ByteBuf.writeByteArray(bytes: ByteArray) {
        writeVarInt(bytes.size)
        writeBytes(bytes)
    }

    fun ByteBuf.writeVarIntArray(array: IntArray?, buffer: ByteBuf) {
        if (array == null) {
            writeVarInt(0)
            return
        }
        writeVarInt(array.size)
        for (element in array) {
            writeVarInt(element)
        }
    }

    fun ByteBuf.writeStringArray(array: Array<String>) {
        writeVarInt(array.size)
        for (element in array) {
            writeSizedString(element)
        }
    }

    fun ByteBuf.writeUniqueId(uniqueId: UUID) {
        writeLong(uniqueId.mostSignificantBits)
        writeLong(uniqueId.leastSignificantBits)
    }

    fun ByteBuf.toByteArray(): ByteArray {
        val bytes = ByteArray(readableBytes())
        val readerIndex = readerIndex()
        getBytes(readerIndex, bytes)
        return bytes
    }
}

class SimplePacketData<T>(private val read: (buffer: ByteBuf)->T, private val write: (buffer: ByteBuf, value: T)->Unit, defaultValue: T? = null): PacketData<T, T>() {
    var value: T? = defaultValue
        private set

    override fun read(buffer: ByteBuf) {
        value = read.invoke(buffer)
    }

    override fun write(buffer: ByteBuf) {
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

class OptionalPacketData<V>(private val data: PacketData<V, out V?>, private val condition: () -> Boolean): PacketData<V, V?>() {

    var match = false

    override fun read(buffer: ByteBuf) {
        match = condition.invoke()
        if (match) {
            data.read(buffer)
        }
    }

    override fun write(buffer: ByteBuf) {
        data.write(buffer)
    }

    override fun getValue(): V? {
        if (!match) return null
        return data.getValue()
    }

    override fun setValue(value: V) {
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