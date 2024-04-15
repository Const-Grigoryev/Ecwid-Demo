package dev.aspid812.ipv4_count.impl

import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.mockito.Mockito

import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions

import java.io.StringReader

import dev.aspid812.ipv4_count.impl.IPv4Parser.ParseResult
import org.junit.jupiter.params.provider.*


class IPv4ParserTest {

	companion object {
		private fun ipStringToInt(addressString: String): Int =
			addressString.splitToSequence('.')
				.map(Integer::parseInt)
				.fold(0) { address, octet -> address.shl(8) + octet }
	}

	@Test
	fun `Test utility sanity check`() {
		assertEquals(0x7F000001,         ipStringToInt("127.0.0.1"))
		assertEquals(0xAC10FE01.toInt(), ipStringToInt("172.16.254.1"))
		assertEquals(0x08666768,         ipStringToInt("08.102.103.104"))
	}


	lateinit var subject: IPv4Parser
	lateinit var sink: IPv4Builder

	@BeforeEach
	fun setup() {
		sink = Mockito.mock(IPv4Builder::class.java)
	}

	@ParameterizedTest
	@ValueSource(strings=["1.2.3.4", "255.255.255.255", "0.0.0.0"])
	fun `Valid IPv4 addresses are parsed correctly`(addressString: String) {
		subject = IPv4Parser(StringReader(addressString))

		val result = subject.parseNextLine(sink)

		val expectedAddress = ipStringToInt(addressString)
		assertEquals(ParseResult.ADDRESS, result)
		verify(sink).accept(expectedAddress)
		verifyNoMoreInteractions(sink)
	}

	@ParameterizedTest
	@ValueSource(strings=["00.01.02.03", "000.001.002.003", "0000.0001.0002.0003"])
	fun `Octet may start with one or more zeros`(addressString: String) {
		subject = IPv4Parser(StringReader(addressString))

		val result = subject.parseNextLine(sink)

		val expectedAddress = ipStringToInt(addressString)
		assertEquals(ParseResult.ADDRESS, result)
		verify(sink).accept(expectedAddress)
		verifyNoMoreInteractions(sink)
	}

	@ParameterizedTest
	@ValueSource(strings=[".2.3.4", ".1.2.3.4", "1..3.4", "1.2.3.", "1.2.3.4."])
	fun `Empty octets are not allowed`(addressString: String) {
		subject = IPv4Parser(StringReader(addressString))

		val result = subject.parseNextLine(sink)

		assertEquals(ParseResult.MISTAKE, result)
		verifyNoInteractions(sink)
	}

	// „Три есть цифирь, до коей счесть потребно, и сочтенья твои суть три. До четырёх счесть не моги,
	// паче же до двух, опричь токмо коли два предшествует трём“.
	@ParameterizedTest
	@ValueSource(strings=["1.2.3", "1.2.3.4.5"])
	fun `IPv4 address must consist of exactly four octets`(addressString: String) {
		subject = IPv4Parser(StringReader(addressString))

		val result = subject.parseNextLine(sink)

		assertEquals(ParseResult.MISTAKE, result)
		verifyNoInteractions(sink)
	}

	@ParameterizedTest
	@ValueSource(strings=["1.2.3.256"])
	fun `Octet value must be between 0 and 255`(addressString: String) {
		subject = IPv4Parser(StringReader(addressString))

		val result = subject.parseNextLine(sink)

		assertEquals(ParseResult.MISTAKE, result)
		verifyNoInteractions(sink)
	}
}