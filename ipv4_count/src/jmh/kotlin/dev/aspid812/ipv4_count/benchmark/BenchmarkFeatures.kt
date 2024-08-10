package dev.aspid812.ipv4_count.benchmark

import java.io.IOException
import java.io.InputStream
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import kotlin.io.path.*

import dev.aspid812.ipv4_count.IPv4Count
import dev.aspid812.ipv4_gen.Sample
import dev.aspid812.ipv4_gen.draw
import dev.aspid812.ipv4_gen.util.Multiplier


object BenchmarkFeatures {

	fun newThrowingErrorHandler() = IPv4Count.ErrorHandler { error ->
		throw IllegalStateException("Sudden syntax error reported: $error")
	}
}


interface InternalDatasetFeaturedBenchmark {

	val dataset: String

	fun openDatasetStream(): InputStream {
		if (dataset.isNotBlank())
			return Path(dataset).inputStream()

		return javaClass.getResourceAsStream("/RandomIPs-1K.txt")
			?: throw IOException("Default dataset is missing!")
	}

	fun loadDataset(): ByteArray {
		return openDatasetStream().use { it.readBytes() }
	}
}


interface ExternalDatasetFeaturedBenchmark {

	companion object Safety {
		const val LARGE_FILE_PROP_KEY = "benchmark.safety.largeFile"
		const val LARGE_FILE_THRESHOLD = 64_000_000L    // 64M lines is approx. 1 GB

		fun checkDatasetHasSaneSize(linesNumber: Long) {
			val enabled = System.getProperty(LARGE_FILE_PROP_KEY)?.toBooleanStrictOrNull() ?: true
			val failed = enabled && linesNumber > LARGE_FILE_THRESHOLD
			check(!failed) {
				"""Safety setting prevents generation of a large file. Set -D$LARGE_FILE_PROP_KEY=false
				>> manually to disable this handy foolproof check.""".trimMargin(">> ")    // Господи, ну что за уродство
			}
		}
	}

	val lines: String

	val linesNumber get() = Multiplier.parseLong(lines)

	fun computeDatasetPath(lines: String): Path {
		val tempDir = Path(System.getProperty("java.io.tmpdir"))
		return tempDir / "RandomIPs-$lines.txt"
	}

	fun writeDatasetFile(datasetPath: Path, sample: Sample) {
		FileChannel.open(datasetPath, CREATE_NEW, WRITE).use { channel ->
			sample.draw(channel)
		}
	}

	fun ensureDatasetPresent(sample: (Long) -> Sample): Path {
		val datasetPath = computeDatasetPath(lines)
		if (!datasetPath.exists()) {
			checkDatasetHasSaneSize(linesNumber)
			writeDatasetFile(datasetPath, sample(linesNumber))
		}
		return datasetPath
	}
}
