package dev.aspid812.ipv4_count

import java.nio.channels.Channels
import java.util.*

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

import dev.aspid812.ipv4_count.IPv4Count.ErrorHandler
import dev.aspid812.ipv4_count.IPv4Count.FailureException


class IPv4CountTest {

	enum class DataSample(vararg lines: String, sep: String = "\n") {
		SOLID(
			"145.67.23.4",
			"8.34.5.23",
			"89.54.3.124",
			"89.54.3.124",
			"3.45.71.5",
			sep = "\r\n"
		),
		SPARCE(
			"8.34.5.23",
			"",
			"3.14.159.26",
			"64.1.20.49",
			""
		),
		CORRUPTED(
			"142.251.1.147",
			"localhost"
		);

		val content = lines.joinToString(sep)

		fun newByteChannel() = Channels.newChannel(content.byteInputStream())
		fun lineSequence() = content.lineSequence()
	}

	lateinit var subject: IPv4Count

	@Test
	fun `Subsequent calls to account() accumulate all accounting data`() {
		val dataSamples = listOf(DataSample.SOLID, DataSample.SPARCE)
		val errorHandler = ErrorHandler { error -> throw RuntimeException(error) }
		subject = IPv4Count(errorHandler)

		for (dataSample in dataSamples) {
			// Neglect channel closing for simplicity, it is safe in this particular case
			subject.account(dataSample.newByteChannel())
		}
		val actualCount = subject.uniqueAddresses()

		val expectedCountInt = dataSamples
			.flatMap { it.lineSequence() }
			.filter { it.isNotEmpty() }
			.distinct()
			.count()
		val expectedCount = OptionalLong.of(expectedCountInt.toLong())
		assertEquals(expectedCount, actualCount)
	}

	@Test
	fun `Exception thrown from the ErrorHandler disables accounting process at all`() {
		val dataSamples = listOf(DataSample.SPARCE, DataSample.CORRUPTED, DataSample.SOLID)
		val errorHandler = ErrorHandler { error -> throw FailureException(error) }
		subject = IPv4Count(errorHandler)

		for (dataSample in dataSamples) {
			// Neglect channel closing for simplicity, it is safe in this particular case
			subject.account(dataSample.newByteChannel())
		}

		assertEquals(OptionalLong.empty(), subject.uniqueAddresses())
	}
}
