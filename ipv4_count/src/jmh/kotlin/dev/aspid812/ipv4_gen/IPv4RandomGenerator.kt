package dev.aspid812.ipv4_gen

import java.nio.ByteBuffer
import kotlin.random.Random

import dev.aspid812.ipv4_count.impl.IPv4Address


class IPv4RandomGenerator internal constructor(
	val rng: Random,
	val pool: List<String>
) {
	companion object {
		const val RECOMMENDED_POOL_SIZE = 64_000
	}

	private val binaryPool = pool.map(String::toByteArray).toTypedArray()

	fun sample(size: Long) = object: Sample {
		var remaining = size

		override val size = size
		override val exhausted get() = remaining == 0L

		override fun draw(output: ByteBuffer): Long {
			val drawn = drawSample(remaining, output)
			remaining -= drawn
			return drawn
		}
	}

	fun drawSample(demand: Long, output: ByteBuffer): Long {
		var supply = 0L
		while (supply < demand) {
			val item = binaryPool.random(rng)
			if (output.remaining() < item.size)
				break
			output.put(item).also { supply++ }
		}
		return supply
	}
}


fun IPv4RandomGenerator(poolSize: Int): IPv4RandomGenerator {
	//TODO: Should we take care of uniqueness of items in the pool?
	val rng = Random.Default
	val pool = List(poolSize) { IPv4Address.toString(rng.nextInt()) + "\n" }
	return IPv4RandomGenerator(rng, pool)
}
