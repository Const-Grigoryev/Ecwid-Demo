package dev.aspid812.ipv4_gen

import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream

import dev.aspid812.ipv4_gen.util.Multiplier


const val EXIT_OK = 0
const val EXIT_FATAL = -1

class IPv4RandomGeneratorApp(
	val poolSize: Int,
	val sampleSize: Long
) {
	companion object {
		const val POOL_SIZE_DEFAULT = IPv4RandomGenerator.RECOMMENDED_POOL_SIZE
		const val SAMPLE_SIZE_DEFAULT = Long.MAX_VALUE      // Effectively endless

		val poolSizeOption   = Regex("-p(?<value>\\d+)")
		val sampleSizeOption = Regex("-n(?<value>\\d+[KMB]?)")

		@JvmStatic
		fun forCommandLine(vararg options: String): IPv4RandomGeneratorApp {
			fun <T> Sequence<T>.singleOrNone() =
				this.reduceOrNull { _, _ -> throw IllegalArgumentException("Repeated option") }

			val poolSize = options.asSequence()
				.mapNotNull { poolSizeOption.matchEntire(it)?.destructured }
				.map { (value) -> value.toInt() }
				.singleOrNone() ?: POOL_SIZE_DEFAULT
			val sampleSize = options.asSequence()
				.mapNotNull { sampleSizeOption.matchEntire(it)?.destructured }
				.map { (value) -> Multiplier.parseLong(value) }
				.singleOrNone() ?: SAMPLE_SIZE_DEFAULT
			return IPv4RandomGeneratorApp(poolSize, sampleSize)
		}
	}

	private val engine = IPv4RandomGenerator(poolSize)

	fun run(output: OutputStream, logger: PrintStream): Int {
		try {
			engine.sample(sampleSize, output)
		}
		catch (ex: IOException) {
			ex.printStackTrace(logger)
			return EXIT_FATAL
		}

		return EXIT_OK
	}
}

fun IPv4RandomGeneratorApp(vararg options: String): IPv4RandomGeneratorApp {
	//TODO: Catch any exception caused by invalid options and wrap it to IAE
	return IPv4RandomGeneratorApp.forCommandLine(*options)
}


fun main(args: Array<String>) {
	val application = IPv4RandomGeneratorApp.forCommandLine(*args)
	val status = application.run(System.out, System.err)
	if (status != EXIT_OK) {
		kotlin.system.exitProcess(status)
	}
}
