package dev.aspid812.ipv4_gen

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream

import dev.aspid812.common.Application
import dev.aspid812.common.Application.EXIT_FATAL
import dev.aspid812.common.Application.EXIT_OK
import dev.aspid812.ipv4_gen.util.Multiplier


class IPv4RandomGeneratorApp(
	val poolSize: Int,
	val sampleSize: Long
) : Application {

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

	override fun run(input: InputStream?, output: PrintStream?, logger: PrintStream?) =
		run(
			output = requireNotNull(output),
			logger = requireNotNull(logger)
		)

	fun run(output: OutputStream, logger: PrintStream): Int {
		try {
			engine.sample(sampleSize).draw(output)
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
