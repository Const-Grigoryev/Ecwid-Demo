package dev.aspid812.ipv4_count.util

import java.io.Reader


class CharIteratorReader(
	private val iterator: CharIterator
) : Reader() {

	override fun read(): Int {
		val iter = iterator.takeIf { it.hasNext() } ?: return -1
		return iter.nextChar().code
	}

	override fun read(destArray: CharArray, destOffset: Int, demand: Int) = TODO("Not yet implemented")

	override fun close() {}
}


private fun Iterator<Char>.unboxed() = object: CharIterator() {
	override fun hasNext() = this@unboxed.hasNext()
	override fun nextChar() = this@unboxed.next()
}


fun CharIterator.reader() = CharIteratorReader(this)
fun Iterator<Char>.reader() = CharIteratorReader(this.unboxed())
