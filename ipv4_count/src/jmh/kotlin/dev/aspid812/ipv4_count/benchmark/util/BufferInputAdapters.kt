package dev.aspid812.ipv4_count.benchmark.util

import java.io.InputStream
import java.io.Reader
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.CharBuffer


interface BufferProvider<out B : Buffer> {
	fun buffer(): B
}


open class BufferHolder<B : Buffer>(
	private val buffer: B
) : BufferProvider<B> {

	override fun buffer(): B {
		if (!buffer.hasRemaining()) {
			refresh(buffer)
		}
		return buffer
	}

	protected open fun refresh(buffer: B) {}
}


class RewindingBufferHolder<B : Buffer>(
	buffer: B
) : BufferHolder<B>(buffer) {

	override fun refresh(buffer: B) {
		buffer.rewind()
	}
}


private fun <B : Buffer> BufferProvider<B>.workingBuffer(): B? {
	val buf = this.buffer()
	return buf.takeIf { it.hasRemaining() }
}

private fun <B : Buffer> BufferProvider<B>.workingBuffer(demand: Int): Pair<B, Int>? {
	val buf = this.buffer()
	val supply = buf.remaining()
	return Pair(buf, minOf(supply, demand)).takeIf { supply > 0 }
}


class ByteBufferInputStream(
	private val provider: BufferProvider<ByteBuffer>
) : InputStream() {

	override fun read(): Int {
		val buf = provider.workingBuffer() ?: return -1
		return buf.get().toUByte().toInt()
	}

	override fun read(destArray: ByteArray, destOffset: Int, demand: Int): Int {
		val (buf, supply) = provider.workingBuffer(demand) ?: return -1
		buf.get(destArray, destOffset, supply)
		return supply
	}

	override fun close() {}
}


class CharBufferReader(
	private val provider: BufferProvider<CharBuffer>
) : Reader() {

	override fun read(): Int {
		val buf = provider.workingBuffer() ?: return -1
		return buf.get().code
	}

	override fun read(destArray: CharArray, destOffset: Int, demand: Int): Int {
		val (buf, supply) = provider.workingBuffer(demand) ?: return -1
		buf.get(destArray, destOffset, supply)
		return supply
	}

	override fun close() {}
}
