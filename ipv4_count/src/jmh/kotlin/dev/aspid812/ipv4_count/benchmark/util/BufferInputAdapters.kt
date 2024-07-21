package dev.aspid812.ipv4_count.benchmark.util

import java.io.InputStream
import java.io.Reader
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.CharBuffer


class BufferKeeper<B : Buffer>(
	private val create: () -> B,
	private val refill: (B) -> B
) {
	private var buffer: B? = create()

	fun get(): B? {
		var buf = buffer
		if (buf != null && !buf.hasRemaining()) {
			buf = refill(buf).takeIf { it.hasRemaining() }
			buffer = buf
		}
		return buf
	}
}


private class ByteBufferInputStream(
	private val bufferKeeper: BufferKeeper<ByteBuffer>
) : InputStream() {

	override fun read(): Int {
		val buf = bufferKeeper.get() ?: return -1
		return buf.get().toUByte().toInt()
	}

	override fun read(destArray: ByteArray, destOffset: Int, demand: Int): Int {
		val buf = bufferKeeper.get() ?: return -1
		val supply = minOf(buf.remaining(), demand)
		buf.get(destArray, destOffset, supply)
		return supply
	}

	override fun close() {}
}


private class CharBufferReader(
	private val bufferKeeper: BufferKeeper<CharBuffer>
) : Reader() {

	override fun read(): Int {
		val buf = bufferKeeper.get() ?: return -1
		return buf.get().code
	}

	override fun read(destArray: CharArray, destOffset: Int, demand: Int): Int {
		val buf = bufferKeeper.get() ?: return -1
		val supply = minOf(buf.remaining(), demand)
		buf.get(destArray, destOffset, supply)
		return supply
	}

	override fun close() {}
}


fun ByteBufferInputStream(create: () -> ByteBuffer, refill: (ByteBuffer) -> ByteBuffer): InputStream =
	ByteBufferInputStream(BufferKeeper(create, refill))

fun CharBufferReader(create: () -> CharBuffer, refill: (CharBuffer) -> CharBuffer): Reader =
	CharBufferReader(BufferKeeper(create, refill))
