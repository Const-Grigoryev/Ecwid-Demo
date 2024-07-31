package dev.aspid812.ipv4_count.benchmark

import java.io.InputStream
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.IntBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import dev.aspid812.ipv4_count.IPv4Count
import dev.aspid812.ipv4_count.benchmark.InternalDatasetFeaturedBenchmark.Companion.NEWLINE
import dev.aspid812.ipv4_count.benchmark.util.BufferKeeper
import dev.aspid812.ipv4_count.benchmark.util.ByteBufferInputStream
import dev.aspid812.ipv4_count.impl.*


@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class BC01_Initialization {

	private val errorHandler = BenchmarkFeatures.newThrowingErrorHandler()
	private val counter = IPv4Count(errorHandler)

	@Benchmark
	fun v1_init(): Long {
		InputStream.nullInputStream().use { source ->
			counter.account(source)
		}
		return counter.uniqueAddresses().asLong
	}
}


@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class BC02_Reader : InternalDatasetFeaturedBenchmark {

	@Param("")
	override lateinit var dataset: String

	lateinit var dataKeeper: BufferKeeper<ByteBuffer>
	lateinit var standardReader: LineNumberReader
	lateinit var lightweightReader: LightweightReader

	@Setup(Level.Iteration)
	fun setup() {
		val data = loadDataset()
		val dataStream = ByteBufferInputStream(
			create = { ByteBuffer.wrap(data).asReadOnlyBuffer() },
			refill = { it.rewind() }
		)
		dataKeeper = BufferKeeper(
			create = { ByteBuffer.wrap(data) },
			refill = { it.rewind() }
		)
		standardReader = LineNumberReader(InputStreamReader(dataStream, StandardCharsets.UTF_8), DEFAULT_BUFFER_SIZE)
		lightweightReader = LightweightInputStreamReader(dataStream)
	}

	@Benchmark
	fun v0(): Int {
		do {
			val buffer = checkNotNull(dataKeeper.get())
			val ch = buffer.get().toInt()
		} while (ch != -1 && ch != NEWLINE)
		return dataKeeper.get()?.remaining() ?: -1
	}

	@Benchmark
	fun v1_standard(): Int {
		do {
			val ch = standardReader.read()
		} while (ch != -1 && ch != NEWLINE)
		return standardReader.lineNumber
	}

	@Benchmark
	fun v2_lightweight(): Boolean {
		do {
			val ch = lightweightReader.read()
		} while (ch != -1 && ch != NEWLINE)
		return lightweightReader.eof()
	}
}


@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class BC03_Parser : InternalDatasetFeaturedBenchmark {

	@Param("")
	override lateinit var dataset: String

	lateinit var dataKeeper: BufferKeeper<CharBuffer>
	lateinit var lineParser: IPv4LineParser

	@Setup(Level.Iteration)
	fun setup() {
		val data = loadDataset().decodeToString().toCharArray()
		dataKeeper = BufferKeeper(
			create = { CharBuffer.wrap(data).asReadOnlyBuffer() },
			refill = { it.rewind() }
		)
		lineParser = IPv4LineParser()
	}

	@Benchmark
	fun v0() {
		val buffer = checkNotNull(dataKeeper.get())
		while (buffer.hasRemaining()) {
			val ch = buffer.get()
			if (ch == '\n')
				break
		}
	}

	@Benchmark
	fun v1_parser(): Int {
		val buffer = checkNotNull(dataKeeper.get())
		val token = lineParser.parseLine(buffer, IPv4LineClassifier.INSTANCE)
		return if (token == IPv4LineClassifier.LineToken.VALID_ADDRESS) lineParser.address else 0xBADF00D
	}
}


@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class BC04_AddressSet :
	InternalDatasetFeaturedBenchmark
{
	@Param("")
	override lateinit var dataset: String

	lateinit var dataKeeper: BufferKeeper<IntBuffer>
	lateinit var bitScale: BitScale

	@Setup(Level.Iteration)
	fun setup() {
		//TODO: Replace with `IPv4LineVisitor`... or keep it as is
		val data = openDatasetStream().reader().useLines { lines -> lines
			.filter { it.isNotEmpty() }
			.map(IPv4Address::parseInt)
			.toList()
			.toIntArray()
		}
		dataKeeper = BufferKeeper(
			create = { IntBuffer.wrap(data) },
			refill = { it.rewind() }
		)
		bitScale = BitScale(1L.shl(IPv4Address.SIZE))
	}

	@TearDown
	fun teardown(blackhole: Blackhole) {
		blackhole.consume(bitScale.count())
	}

	@Benchmark
	fun v0(): Int {
		val buffer = checkNotNull(dataKeeper.get())
		return buffer.get()
	}

	@Benchmark
	fun v1_bitScale() {
		val buffer = checkNotNull(dataKeeper.get())
		bitScale.witness(buffer.get().toUInt().toLong())
	}
}
