package dev.aspid812.ipv4_count.benchmark

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import dev.aspid812.ipv4_count.IPv4CountApp
import dev.aspid812.ipv4_count.benchmark.util.PrepareProcess
import dev.aspid812.ipv4_count.benchmark.util.BlackholeOutputStream
import dev.aspid812.ipv4_gen.util.Multiplier
import dev.aspid812.ipv4_gen.main as IPv4RandomGeneratorApp_main


@State(Scope.Thread)
abstract class IntegralBenchmarkBase :
	GeneratorFeaturedBenchmark by BenchmarkFeatures.generator()
{
	companion object Safety {
		const val LARGE_FILE_PROP_KEY = "benchmark.safety.largeFile"
		const val LARGE_FILE_THRESHOLD = 64_000_000L    // 64M lines is approx. 1 GB
	}

	abstract val lines: String

	open val physicalInputFile = true

	var inputFilePath = null as Path?

	val linesAsLong: Long
		get() = Multiplier.parseLong(lines)

	val inputFile: File
		get() = inputFilePath?.toFile() ?: TODO("Return /dev/null or NUL, depending on the user OS")

	fun largeFileSafetyCheck(): Boolean {
		val enabled = System.getProperty(LARGE_FILE_PROP_KEY)?.toBooleanStrictOrNull() ?: true
		val failed = enabled && linesAsLong > LARGE_FILE_THRESHOLD
		if (failed) {
			System.err.println(
				"""Safety setting prevents generation of a large file. Set -D$LARGE_FILE_PROP_KEY=false
				manually to disable this handy foolproof check."""
			)
		}
		return !failed
	}

	@Setup
	fun setup() {
		if (physicalInputFile) {
			val tempDirPath = Path.of(System.getProperty("java.io.tmpdir"))
			val filePath = tempDirPath.resolve("RandomIPs-$lines.txt")
			if (Files.notExists(filePath) && largeFileSafetyCheck()) {
				Files.newOutputStream(filePath).use { output ->
					generator.sample(linesAsLong, output)
				}
			}
			inputFilePath = filePath
		}
	}
}


@Fork(1)
@BenchmarkMode(Mode.SingleShotTime)
@State(Scope.Thread)
open class ApplicationBenchmark : IntegralBenchmarkBase() {

	companion object {
		// Since „callable references to top-level functions don't support fully-qualified syntax right now,“
		// (R. Elizarov, https://youtrack.jetbrains.com/issue/KT-52701), we have to work-around by aliasing import
		val IPv4_GENERATOR_APP = ::IPv4RandomGeneratorApp_main
		val IPv4_COUNT_APP = IPv4CountApp::main
	}

	@Param("1K", "25K", "1M", "16M")
	override lateinit var lines: String

	@Benchmark
	fun n00_generateOnly() {
		val process = PrepareProcess.java(IPv4_GENERATOR_APP, "-n$lines")
			.redirectOutput(Redirect.DISCARD)
			.start()
		process.waitFor()
	}

	@Benchmark
	fun n01_liveInput() {
		val processes = ProcessBuilder.startPipeline(listOf(
			PrepareProcess.java(IPv4_GENERATOR_APP, "-n$lines"),
			PrepareProcess.java(IPv4_COUNT_APP)
				.redirectOutput(Redirect.DISCARD)
		))
		processes.last().waitFor()
	}

	@Benchmark
	fun n02_fileInput() {
		val process = PrepareProcess.java(IPv4_COUNT_APP)
			.redirectInput(inputFile)
			.redirectOutput(Redirect.DISCARD)
			.start()
		process.waitFor()
	}
}


@Fork(1)
@BenchmarkMode(Mode.SingleShotTime)
@State(Scope.Thread)
open class EngineBenchmark :
	IntegralBenchmarkBase(),
	CounterFeaturedBenchmark by BenchmarkFeatures.counter()
{
	@Param("1K", "25K", "1M", "16M")
	override lateinit var lines: String

	@Benchmark
	fun n00_generateOnly(blackhole: Blackhole) {
		generator.newInputStream(linesAsLong).use { source ->
			val destination = BlackholeOutputStream(blackhole)
			source.transferTo(destination)
		}
	}

	@Benchmark
	fun n01_liveInput(): Long {
		generator.newInputStream(linesAsLong).use { source ->
			return counter.countUnique(source, newErrorHandler())
		}
	}

	@Benchmark
	fun n02_fileInput(): Long {
		Files.newInputStream(inputFile.toPath()).use { source ->
			return counter.countUnique(source, newErrorHandler())
		}
	}
}


@Fork(1)
@BenchmarkMode(Mode.SingleShotTime)
@State(Scope.Thread)
open class LoadBenchmark :
	GeneratorFeaturedBenchmark by BenchmarkFeatures.generator(),
	CounterFeaturedBenchmark by BenchmarkFeatures.counter()
{
	private fun measure(lines: Long): Long {
		require(lines >= 0)
		generator.newInputStream(lines).use { source ->
			return counter.countUnique(source, newErrorHandler())
		}
	}

	@Warmup(iterations = 5)
	@Measurement(iterations = 15)
	@Benchmark
	fun n01_measure_1k() = measure(1_000)

	@Warmup(iterations = 3)
	@Measurement(iterations = 10)
	@Benchmark
	fun n02_measure_25k() = measure(25_000)

	@Warmup(iterations = 1)
	@Measurement(iterations = 5)
	@Benchmark
	fun n03_measure_1m() = measure(1_000_000)

	@Warmup(iterations = 1)
	@Measurement(iterations = 3)
	@Benchmark
	fun n04_measure_16m() = measure(16_000_000)

	@Benchmark
	fun n05_measure_200m() = measure(200_000_000)

	@Timeout(time = 4, timeUnit = TimeUnit.HOURS)
	@Benchmark
	fun n06_measure_8b() = measure(8_000_000_000)
}
