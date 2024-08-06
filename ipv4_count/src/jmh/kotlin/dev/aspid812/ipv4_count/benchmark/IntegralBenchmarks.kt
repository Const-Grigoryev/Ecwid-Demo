package dev.aspid812.ipv4_count.benchmark

import java.lang.ProcessBuilder.Redirect
import java.nio.ByteBuffer
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
import java.nio.channels.Channels


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

	private val errorHandler = BenchmarkFeatures.newThrowingErrorHandler()
	private val generator = IPv4RandomGenerator(RECOMMENDED_POOL_SIZE)
	private val counter = IPv4Count(errorHandler)

	@Param("1K", "25K", "1M", "16M")
	override lateinit var lines: String

	lateinit var datasetPath: Path

	@Setup
	fun setup() {
		datasetPath = ensureDatasetPresent(generator::sample)
	}

	@Benchmark
	fun v1_application_stdin() {
		val process = PrepareProcess.java(IPv4_COUNT_APP)
			.redirectInput(datasetPath.toFile())
			.redirectOutput(Redirect.DISCARD)
			.start()
		process.waitFor()
	}

	@Benchmark
	fun v2_application_file() {
		val process = PrepareProcess.java(IPv4_COUNT_APP, datasetPath.toString())
			.redirectOutput(Redirect.DISCARD)
			.start()
		process.waitFor()
	}

	@Benchmark
	fun v3_library_file(): Long {
		counter.account(datasetPath)
		return counter.uniqueAddresses().asLong
	}

	@Benchmark
	fun v4_library_gen(): Long {
		generator.openByteChannel(linesNumber).use { input ->
			counter.account(input)
		}
		return counter.uniqueAddresses().asLong
	}
}


@State(Scope.Thread)
@Fork(1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.SECONDS)
open class BI02_Performance : ExternalDatasetFeaturedBenchmark {

	private val errorHandler = BenchmarkFeatures.newThrowingErrorHandler()
	private val generator = IPv4RandomGenerator(RECOMMENDED_POOL_SIZE)
	private val counter = IPv4Count(errorHandler)

	@Param("1K", "25K", "1M", "16M", "200M", "8B")
	override lateinit var lines: String

	@Benchmark
	fun v1_measure(): Long {
		generator.openByteChannel(linesNumber).use { input ->
			counter.account(input)
		}
		return counter.uniqueAddresses().asLong
	}
}
