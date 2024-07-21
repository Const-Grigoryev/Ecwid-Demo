package dev.aspid812.ipv4_count.benchmark

import java.lang.ProcessBuilder.Redirect
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.inputStream

import org.openjdk.jmh.annotations.*

import dev.aspid812.ipv4_count.IPv4Count
import dev.aspid812.ipv4_count.IPv4CountApp
import dev.aspid812.ipv4_count.benchmark.util.ByteBufferInputStream
import dev.aspid812.ipv4_count.benchmark.util.PrepareProcess
import dev.aspid812.ipv4_gen.IPv4RandomGenerator
import dev.aspid812.ipv4_gen.IPv4RandomGenerator.Companion.RECOMMENDED_POOL_SIZE
import dev.aspid812.ipv4_gen.main as IPv4RandomGeneratorApp_main


@State(Scope.Thread)
@Fork(1)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class BI01_ModesComparison : ExternalDatasetFeaturedBenchmark {

	companion object {
		// Since „callable references to top-level functions don't support fully-qualified syntax right now,“
		// (R. Elizarov, https://youtrack.jetbrains.com/issue/KT-52701), we have to work-around by aliasing import
		val IPv4_GENERATOR_APP = ::IPv4RandomGeneratorApp_main
		val IPv4_COUNT_APP = IPv4CountApp::main
	}

	private val generator = IPv4RandomGenerator(RECOMMENDED_POOL_SIZE)
	private val counter = IPv4Count()

	open fun newErrorHandler() = BenchmarkFeatures.newThrowingErrorHandler()

	@Param("1K", "25K", "1M", "16M")
	override lateinit var lines: String

	lateinit var datasetPath: Path

	@Setup
	fun setup() {
		datasetPath = ensureDatasetPresent(generator::sample)
	}

	@Benchmark
	fun v1_application() {
		val process = PrepareProcess.java(IPv4_COUNT_APP)
			.redirectInput(datasetPath.toFile())
			.redirectOutput(Redirect.DISCARD)
			.start()
		process.waitFor()
	}

	@Benchmark
	fun v2_library_file(): Long {
		datasetPath.inputStream().buffered().use { input ->
			return counter.countUnique(input, newErrorHandler())
		}
	}

	@Benchmark
	fun v3_library_nio(): Long {
		FileChannel.open(datasetPath).use { channel ->
			val stream = ByteBufferInputStream(
				create = { ByteBuffer.allocate(DEFAULT_BUFFER_SIZE).limit(0) },
				refill = { it.clear().also(channel::read).flip() }
			)
			return counter.countUnique(stream, newErrorHandler())
		}
	}

	@Benchmark
	fun v4_library_gen(): Long {
		generator.openInputStream(linesNumber).use { input ->
			return counter.countUnique(input, newErrorHandler())
		}
	}
}


@State(Scope.Thread)
@Fork(1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.SECONDS)
open class BI02_Performance : ExternalDatasetFeaturedBenchmark {

	private val generator = IPv4RandomGenerator(RECOMMENDED_POOL_SIZE)
	private val counter = IPv4Count()

	open fun newErrorHandler() = BenchmarkFeatures.newThrowingErrorHandler()

	@Param("1K", "25K", "1M", "16M", "200M", "8B")
	override lateinit var lines: String

	@Benchmark
	fun v1_measure(): Long {
		generator.openInputStream(linesNumber).use { input ->
			return counter.countUnique(input, newErrorHandler())
		}
	}
}
