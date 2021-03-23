package de.dseelp.kommon.network.codec

import de.dseelp.kommon.network.codec.packet.BufferUtils
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.handler.codec.CorruptedFrameException

class PacketFramer: ByteToMessageCodec<ByteBuf>() {
    private val maxPacketSize = 30_000
    override fun encode(ctx: ChannelHandlerContext, from: ByteBuf, to: ByteBuf) {
        frame(from, to)
    }

    private fun frame(packetBuffer: ByteBuf, frameTarget: ByteBuf) {
        val packetSize = packetBuffer.readableBytes()
        val headerSize: Int = getVarIntSize(packetSize)
        check(headerSize <= 3) { "Unable to fit $headerSize into 3" }
        frameTarget.ensureWritable(packetSize + headerSize)
        BufferUtils.writeVarInt(packetSize, frameTarget)
        frameTarget.writeBytes(packetBuffer, packetBuffer.readerIndex(), packetSize)
    }

    private fun getVarIntSize(input: Int): Int = when {
        input and -0x80 == 0 -> 1
        input and -0x4000 == 0 -> 2
        input and -0x200000 == 0 -> 3
        input and -0x10000000 == 0 -> 4
        else -> 5
    }

    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        buffer.markReaderIndex()
        for (i in 0..2) {
            if (!buffer.isReadable) {
                buffer.resetReaderIndex()
                return
            }
            val b = buffer.readByte()
            if (b >= 0) {
                buffer.resetReaderIndex()
                val packetSize: Int = BufferUtils.readVarInt(buffer)

                // Max packet size check
                if (packetSize >= maxPacketSize) {
                    System.err.println("Sent a packet over the maximum size ($packetSize)")
                    ctx.close()
                    //TODO: ADD EVENT FOR CONNECTION THAT SENDED OVER THE MAX PACKET SIZE
                    return
                }
                if (buffer.readableBytes() < packetSize) {
                    buffer.resetReaderIndex()
                    return
                }
                out.add(buffer.readRetainedSlice(packetSize))
                return
            }
        }
        throw CorruptedFrameException("length wider than 21-bit")
    }
}