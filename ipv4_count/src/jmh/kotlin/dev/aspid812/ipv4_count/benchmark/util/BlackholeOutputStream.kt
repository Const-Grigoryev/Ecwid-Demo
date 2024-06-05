package dev.aspid812.ipv4_count.benchmark.util

import org.openjdk.jmh.infra.Blackhole
import org.openjdk.jmh.util.NullOutputStream


class BlackholeOutputStream(
	private val blackhole: Blackhole
) : NullOutputStream() {

	override fun write(array: ByteArray) {
		write(array, 0, array.size)
	}

	override fun write(array: ByteArray, offset: Int, length: Int) {
		blackhole.consume(array)
		if (length > 0) {
			blackhole.consume(array.random())
		}
	}

	override fun write(byte: Int) {
		blackhole.consume(byte)
	}
}