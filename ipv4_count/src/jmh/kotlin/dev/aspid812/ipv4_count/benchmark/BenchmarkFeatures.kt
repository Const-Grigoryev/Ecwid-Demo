package dev.aspid812.ipv4_count.benchmark

import java.io.InputStream
import java.nio.ByteBuffer

import dev.aspid812.ipv4_count.IPv4Count
import dev.aspid812.ipv4_count.benchmark.util.BufferHolder
import dev.aspid812.ipv4_count.benchmark.util.ByteBufferInputStream
import dev.aspid812.ipv4_gen.IPv4RandomGenerator
import dev.aspid812.ipv4_gen.IPv4RandomGenerator.Companion.RECOMMENDED_POOL_SIZE


interface GeneratorFeaturedBenchmark {
	val generator: IPv4RandomGenerator

	fun IPv4RandomGenerator.newInputStream(lineLimit: Long = Long.MAX_VALUE): InputStream {
		val bufferHolder = object: BufferHolder<ByteBuffer>(
			buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE).limit(0)
		) {
			var remainingLines = lineLimit

			override fun refresh(buffer: ByteBuffer) {
				buffer.clear()
				remainingLines -= sample(remainingLines, buffer)
				buffer.flip()
			}
		}
		return ByteBufferInputStream(bufferHolder)
	}
}


interface CounterFeaturedBenchmark {
	val counter: IPv4Count

	fun newErrorHandler() = IPv4Count.ErrorHandler { error ->
		throw IllegalStateException("Sudden syntax error reported: $error")
	}
}


object BenchmarkFeatures {
	fun generator() = object: GeneratorFeaturedBenchmark {
		override val generator = IPv4RandomGenerator(RECOMMENDED_POOL_SIZE)
	}

	fun counter() = object: CounterFeaturedBenchmark {
		override val counter = IPv4Count()
	}
}
