package dev.aspid812.ipv4_count.impl

import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.mockito.Mockito

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.*
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions

import java.io.Reader
import java.io.StringReader

import dev.aspid812.ipv4_count.impl.IPv4Parser.ParseResult


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

	@Nested
	@DisplayName("IPv4 syntax support")
	inner class IPv4Syntax {

		// „Три есть цифирь, до коей счесть потребно, и сочтенья твои суть три. До четырёх счесть не моги,
		// паче же до двух, опричь токмо коли два предшествует трём“.
		@ParameterizedTest
		@ValueSource(strings = ["", "1.2.3", "1.2.3.4.5"])
		fun `IPv4 address consist of exactly four octets`(addressString: String) {
			subject = IPv4Parser(StringReader(addressString))

			val result = subject.parseNextLine(sink)

			assertNotEquals(ParseResult.ADDRESS, result)
			verifyNoInteractions(sink)
		}

		@ParameterizedTest
		@ValueSource(strings = ["256.2.3.4", "1.256.3.4", "1.2.3.256"])
		fun `Octet is a non-negative integer not greater then 255`(addressString: String) {
			subject = IPv4Parser(StringReader(addressString))

			val result = subject.parseNextLine(sink)

			assertNotEquals(ParseResult.ADDRESS, result)
			verifyNoInteractions(sink)
		}

		@ParameterizedTest
		@ValueSource(strings = ["00.01.02.03", "000.001.002.003", "0000.0001.0002.0003"])
		fun `Octet may start with one or more zeros`(addressString: String) {
			subject = IPv4Parser(StringReader(addressString))

			val result = subject.parseNextLine(sink)

			val expectedAddress = ipStringToInt(addressString)
			assertEquals(ParseResult.ADDRESS, result)
			verify(sink).accept(expectedAddress)
			verifyNoMoreInteractions(sink)
		}

		@ParameterizedTest
		@ValueSource(strings = [".2.3.4", ".1.2.3.4", "1..3.4", "1.2.3.", "1.2.3.4."])
		fun `Empty octets are not allowed`(addressString: String) {
			subject = IPv4Parser(StringReader(addressString))

			val result = subject.parseNextLine(sink)

			assertNotEquals(ParseResult.ADDRESS, result)
			verifyNoInteractions(sink)
		}

		@ParameterizedTest
		@ValueSource(strings = ["3.14.159.26", "0.0.0.0", "1.2.3.4", "255.255.255.255"])
		fun `Examples of a well-formed IPv4 address`(addressString: String) {
			subject = IPv4Parser(StringReader(addressString))

			val result = subject.parseNextLine(sink)

			val expectedAddress = ipStringToInt(addressString)
			assertEquals(ParseResult.ADDRESS, result)
			verify(sink).accept(expectedAddress)
			verifyNoMoreInteractions(sink)
		}
	}

	@Nested
	@DisplayName("Multi-line input treatment")
	inner class MultiLineInput {

		@ParameterizedTest
		@ValueSource(strings = ["", "\n", "\n1.2.3.4", "\nhere be dragons"])
		fun `Parser identifies and accepts blank lines`(inputString: String) {
			subject = IPv4Parser(StringReader(inputString))

			val result = subject.parseNextLine(sink)

			assertEquals(ParseResult.NOTHING, result)
			verifyNoInteractions(sink)
		}

		@Test
		fun `Reading beyond end-of-file yields a specific token`() {
			val reads = IntRange(1, 5).toList()
			subject = IPv4Parser(Reader.nullReader())

			val actualResult = reads.map { subject.parseNextLine(sink) }

			val expectedResult = reads.map { ParseResult.NOTHING }
			assertEquals(expectedResult, actualResult)
		}

		@ParameterizedTest
		@ValueSource(strings = ["", "1", "1.2.", "1.2.3.4", "1.2.3.4.", "1.2.3.4.5", "999.0.0.0", "some other stuff"])
		fun `parseNextLine() consumes a single line regardless of its content`(firstLine: String) {
			val secondLine = "3.14.159.26"
			subject = IPv4Parser(StringReader("$firstLine\n$secondLine"))

			val result = arrayOf(
				subject.parseNextLine(sink),
				subject.parseNextLine(sink)
			)

			assertEquals(ParseResult.ADDRESS, result[1])
		}
	}
}