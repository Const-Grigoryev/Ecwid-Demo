package dev.aspid812.ipv4_count.benchmark

import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import dev.aspid812.ipv4_count.IPv4Count
import dev.aspid812.ipv4_count.impl.*


@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class BC01_Initialization {

	@Benchmark
	fun v1_init(): Long {
		val errorHandler = BenchmarkFeatures.newThrowingErrorHandler()
		val counter = IPv4Count(errorHandler)
		return counter.uniqueAddresses().asLong
	}
}


@State(Scope.Thread)
@Warmup(iterations = 5)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class BC02_Parser : InternalDatasetFeaturedBenchmark {

	companion object {
		const val NEWLINE = '\n'.code.toByte()
	}

	@Param("")
	override lateinit var dataset: String

	lateinit var dataBuffer: ByteBuffer
	lateinit var lineParser: IPv4LineParser

	private fun dataBuffer(): ByteBuffer {
		if (dataBuffer.hasRemaining())
			return dataBuffer

		return dataBuffer.rewind()
	}

	@Setup(Level.Iteration)
	fun setup() {
		val data = loadDataset()
		dataBuffer = ByteBuffer.wrap(data).asReadOnlyBuffer()
		lineParser = IPv4LineParser()
	}

	@Benchmark
	fun v0() {
		val buffer = dataBuffer()
		var ch = 0.toByte()
		while (buffer.hasRemaining() && ch != NEWLINE) {
			ch = buffer.get()
		}
	}

	@Benchmark
	fun v1_parser(blackhole: Blackhole) {
		val buffer = dataBuffer()
		val ready = lineParser.parseLine(buffer)
		if (ready && lineParser.classify() == IPv4LineParser.LineToken.VALID_ADDRESS) {
			blackhole.consume(lineParser.address)
		}
	}
}


@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class BC03_AddressSet : InternalDatasetFeaturedBenchmark {

	@Param("")
	override lateinit var dataset: String

	lateinit var dataBuffer: IntBuffer
	lateinit var bitScale: BitScale

	private fun dataBuffer(): IntBuffer {
		if (dataBuffer.hasRemaining())
			return dataBuffer

		return dataBuffer.rewind()
	}

	@Setup(Level.Iteration)
	fun setup() {
		//TODO: Engage `IPv4LineParser` here... or keep it as is
		val data = openDatasetStream().reader().useLines { lines -> lines
			.filter { it.isNotEmpty() }
			.map(IPv4Address::parseInt)
			.toList()
			.toIntArray()
		}
		dataBuffer = IntBuffer.wrap(data).asReadOnlyBuffer()
		bitScale = BitScale(1L shl IPv4Address.SIZE)
	}

	@TearDown
	fun teardown(blackhole: Blackhole) {
		blackhole.consume(bitScale.count())
	}

	@Benchmark
	fun v0(): Int {
		val buffer = dataBuffer()
		return buffer.get()
	}

	@Benchmark
	fun v1_bitScale() {
		val buffer = dataBuffer()
		bitScale.witness(buffer.get().toUInt().toLong())
	}
}
