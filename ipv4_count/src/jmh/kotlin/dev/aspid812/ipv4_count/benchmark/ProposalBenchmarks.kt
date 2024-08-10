package dev.aspid812.ipv4_count.benchmark

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import dev.aspid812.ipv4_gen.IPv4RandomGenerator
import dev.aspid812.ipv4_gen.IPv4RandomGenerator.Companion.RECOMMENDED_POOL_SIZE


@State(Scope.Thread)
@Fork(1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class BP01_Concurrent : ExternalDatasetFeaturedBenchmark {

	companion object {
		const val NEWLINE = '\n'.code.toByte()

		const val SEQUENTIAL_BUFFER_SIZE = 1 shl 20
		const val MAPPING_ALIGNMENT = 4L shl 10
		const val MAPPING_SLICE_SIZE = 8L shl 20
	}

	private val generator = IPv4RandomGenerator(RECOMMENDED_POOL_SIZE)

	@Param("1M", "16M", "200M")
	override lateinit var lines: String

	lateinit var datasetPath: Path

	@Setup
	fun setup() {
		datasetPath = ensureDatasetPresent(generator::sample)
	}

	private fun countLines(buffer: ByteBuffer): Long {
		var count = 0L
		while (buffer.hasRemaining()) {
			if (buffer.get() == NEWLINE) {
				count++
			}
		}
		return count
	}

	@Benchmark
	fun v01_sequential(): Long {
		var count = 0L
		FileChannel.open(datasetPath).use { channel ->
			val buffer = ByteBuffer.allocateDirect(SEQUENTIAL_BUFFER_SIZE)
			while (channel.read(buffer) != -1) {
				buffer.flip()
				count += countLines(buffer)
				buffer.clear()
			}
		}
		return count
	}

	@Benchmark
	fun v02_mapping(): Long {
		fun createTask(channel: FileChannel, position: Long, length: Long): ForkJoinTask<Long> = ForkJoinTask.adapt<Long> {
			if (length <= MAPPING_SLICE_SIZE) {
				val buffer = channel.map(FileChannel.MapMode.READ_ONLY, position, length)
				countLines(buffer)
			}
			else {
				val half = (length / 2) and (MAPPING_ALIGNMENT - 1).inv()
				val subtask1 = createTask(channel, position, half).fork()
				val subtask2 = createTask(channel, position + half, length - half).fork()
				subtask1.join() + subtask2.join()
			}
		}

		val forkJoinPool = ForkJoinPool()
		FileChannel.open(datasetPath).use { channel ->
			return forkJoinPool.invoke(createTask(channel, 0, channel.size()))
		}
	}
}
