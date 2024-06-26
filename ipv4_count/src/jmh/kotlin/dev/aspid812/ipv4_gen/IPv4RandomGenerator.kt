package dev.aspid812.ipv4_gen

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.random.Random


class IPv4RandomGenerator internal constructor(
	val rng: Random,
	val pool: List<String>
) {
	companion object {
		const val RECOMMENDED_POOL_SIZE = 64000
	}

	private val binaryPool = pool.map(String::toByteArray).toTypedArray()
	private val textPool = pool.map(String::toCharArray).toTypedArray()

	@Throws(IOException::class)
	fun sample(demand: Long, output: OutputStream) {
		var supply = 0L
		while (supply < demand) {
			val item = binaryPool.random(rng)
			output.write(item).also { supply++ }
		}
		output.flush()
	}

	fun sample(demand: Long, output: ByteBuffer): Long {
		var supply = 0L
		while (supply < demand) {
			val item = binaryPool.random(rng)
			if (output.remaining() < item.size)
				break
			output.put(item).also { supply++ }
		}
		return supply
	}

	fun sample(demand: Long, output: CharBuffer): Long {
		var supply = 0L
		while (supply < demand) {
			val item = textPool.random(rng)
			if (output.remaining() < item.size)
				break
			output.put(item).also { supply++ }
		}
		return supply
	}
}


private const val OCTET_BITSIZE = 8
private const val OCTET_MASK = (-1).shl(OCTET_BITSIZE).inv()

private fun renderIPv4(address: Int) = sequenceOf(3, 2, 1, 0)
	.map { place -> address.shr(place * OCTET_BITSIZE) and OCTET_MASK }
	.joinToString(".")

fun IPv4RandomGenerator(poolSize: Int): IPv4RandomGenerator {
	//TODO: Should we take care of uniqueness of items in the pool?
	val rng = Random.Default
	val pool = List(poolSize) { renderIPv4(rng.nextInt()) + "\n" }
	return IPv4RandomGenerator(rng, pool)
}
