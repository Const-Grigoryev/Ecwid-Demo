package dev.aspid812.ipv4_count.benchmark

import java.io.InputStream
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.IntBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import dev.aspid812.ipv4_count.benchmark.util.BufferProvider
import dev.aspid812.ipv4_count.benchmark.util.ByteBufferInputStream
import dev.aspid812.ipv4_count.benchmark.util.CharBufferReader
import dev.aspid812.ipv4_count.benchmark.util.RewindingBufferHolder
import dev.aspid812.ipv4_count.impl.*


@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class InitBenchmark :
	GeneratorFeaturedBenchmark by BenchmarkFeatures.generator(),
	CounterFeaturedBenchmark by BenchmarkFeatures.counter()
{
	@Benchmark
	fun measure(): Long {
		generator.newInputStream(0L).use { source ->
			return counter.countUnique(source, newErrorHandler())
		}
	}
}


@State(Scope.Thread)
abstract class ComponentBenchmarkBase {

	@Param("")
	lateinit var dataset: String

	val datasetAsRawBytes: ByteArray by lazy { loadDataset() }

	val datasetAsText: String by lazy { datasetAsRawBytes.toString(StandardCharsets.UTF_8) }

	val datasetAsLineList: List<String> by lazy { datasetAsText.lines() }

	val datasetAsAddressList: List<Int> by lazy {
		datasetAsLineList.asSequence()
			.map { runCatching { IPv4Address.parseInt(it) } }
			.mapNotNull(Result<Int>::getOrNull)
			.toList()
	}

	fun loadDataset(): ByteArray {
		val inputStream = when {
			dataset.isNotBlank() -> Files.newInputStream(Path.of(dataset))
			else -> javaClass.getResourceAsStream("/RandomIPs-1K.txt") ?: throw RuntimeException()
		}
		return inputStream.use(InputStream::readAllBytes)
	}
}


@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class ReaderBenchmark : ComponentBenchmarkBase() {

	companion object {
		private const val LINE_FEED = '\n'.code
	}

//	@Param("$DEFAULT_BUFFER_SIZE", "${0x100000}")
	var buffer: Int = 0x100000

//	lateinit var subject: LineNumberReader
	lateinit var subject: LightweightReader

	@Setup(Level.Iteration)
	fun setup() {
		val buffer = ByteBuffer.wrap(datasetAsRawBytes)
		val stream = ByteBufferInputStream(RewindingBufferHolder(buffer))
//		val reader = InputStreamReader(stream, "UTF-8")
//		subject = LineNumberReader(reader, this.buffer)
		subject = LightweightInputStreamReader(stream)
	}

	@Benchmark
	fun measure(): Boolean {
		do {
			val ch = subject.read()
		} while (ch != LINE_FEED && ch != -1)
//		return subject.lineNumber
		return subject.eof()
	}
}


@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class ParserBenchmark : ComponentBenchmarkBase() {

	lateinit var reader: Reader
	lateinit var subject: MutableIPv4Line

	@Setup(Level.Iteration)
	fun setup() {
		val buffer = CharBuffer.wrap(datasetAsText.toCharArray())
		reader = CharBufferReader(RewindingBufferHolder(buffer))
		subject = MutableIPv4Line()
	}

	@Benchmark
	fun measure(blackhole: Blackhole) {
		blackhole.consume(subject.parseLine(reader))
		blackhole.consume(subject.address)
	}
}


@Warmup(iterations = 3)
@Measurement(iterations = 12)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class BitScaleBenchmark : ComponentBenchmarkBase() {

	lateinit var subject: BitScale

	lateinit var input: BufferProvider<IntBuffer>

	@Setup(Level.Iteration)
	fun setup() {
		val buffer = IntBuffer.wrap(datasetAsAddressList.toIntArray())
		input = RewindingBufferHolder(buffer)
		subject = BitScale(1L.shl(IPv4Address.SIZE))
	}

	@TearDown
	fun teardown(blackhole: Blackhole) {
		val result = subject.count()
		blackhole.consume(result)
	}

	@Benchmark
	fun measure() {
		val value = input.buffer().get()
		subject.witness(value.toUInt().toLong())
	}
}
