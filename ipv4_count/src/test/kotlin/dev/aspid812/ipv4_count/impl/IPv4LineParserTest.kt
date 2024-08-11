package dev.aspid812.ipv4_count.impl

import java.nio.ByteBuffer
import java.nio.charset.Charset

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.*
import org.junit.jupiter.params.ParameterizedTest
import dev.aspid812.ipv4_count.impl.IPv4LineParser.LineToken


class IPv4LineParserTest {

	val charset: Charset = Charsets.UTF_8

	lateinit var subject: IPv4LineParser

	@BeforeEach
	fun setup() {
		subject = IPv4LineParser()
	}

	@Nested
	@DisplayName("API features")
	inner class ApiFeatures {

		@ParameterizedTest
		@ValueSource(strings = ["3.14.15\\\\9.26", "\\1.2\\.\\3.4\\", "127.\\0\\0\\0\\.000.001"])
		fun `Input string may be read by pieces`(inputString: String) {
			val stringPieces = inputString.split("\\").plusElement("\n")

			val actualResults = List(stringPieces.size) { i ->
				val piece = stringPieces[i]
				subject.parseLine(charset.encode(piece))
			}

			val expectedResults = List(stringPieces.size) { it == stringPieces.lastIndex }
			assertEquals(expectedResults, actualResults)

			val expectedValue = inputString.replace("\\", "")
				.let(IPv4Address::parseInt)
			assertEquals(expectedValue, subject.address)
		}
	}

	@Nested
	@DisplayName("IPv4 syntax support")
	inner class IPv4Syntax {

		// „Три есть цифирь, до коей счесть потребно, и сочтенья твои суть три. До четырёх счесть не моги,
		// паче же до двух, опричь токмо коли два предшествует трём“.
		@ParameterizedTest
		@ValueSource(strings = ["1", "1.2.3", "1.2.3.4.5"])
		fun `IPv4 address consist of exactly four octets`(addressString: String) {
			val buffer = charset.encode("$addressString\n")

			subject.parseLine(buffer)

			assertEquals(LineToken.IRRELEVANT_CONTENT, subject.classify())
		}

		@ParameterizedTest
		@ValueSource(strings = ["256.2.3.4", "1.256.3.4", "1.2.3.256"])
		fun `Octet is a non-negative integer not greater then 255`(addressString: String) {
			val buffer = charset.encode("$addressString\n")

			subject.parseLine(buffer)

			assertEquals(LineToken.IRRELEVANT_CONTENT, subject.classify())
		}

		@ParameterizedTest
		@ValueSource(strings = ["00.01.02.03", "000.001.002.003", "0000.0001.0002.0003"])
		fun `Octet may start with one or more zeros`(addressString: String) {
			val buffer = charset.encode("$addressString\n")

			subject.parseLine(buffer)

			val expectedAddress = IPv4Address.parseInt(addressString)
			assertEquals(LineToken.VALID_ADDRESS, subject.classify())
			assertEquals(expectedAddress, subject.address)
		}

		@ParameterizedTest
		@ValueSource(strings = [".2.3.4", ".1.2.3.4", "1..3.4", "1.2.3.", "1.2.3.4."])
		fun `Empty octets are not allowed`(addressString: String) {
			val buffer = charset.encode("$addressString\n")

			subject.parseLine(buffer)

			assertEquals(LineToken.IRRELEVANT_CONTENT, subject.classify())
		}

		@ParameterizedTest
		@ValueSource(strings = ["3.14.159.26", "0.0.0.0", "1.2.3.4", "255.255.255.255"])
		fun `Examples of a well-formed IPv4 address`(addressString: String) {
			val buffer = charset.encode("$addressString\n")

			subject.parseLine(buffer)

			val expectedAddress = IPv4Address.parseInt(addressString)
			assertEquals(LineToken.VALID_ADDRESS, subject.classify())
			assertEquals(expectedAddress, subject.address)
		}
	}

	@Nested
	@DisplayName("Multi-line input handling")
	inner class MultiLineInput {

		@ParameterizedTest
		@ValueSource(strings = ["\n", "\n1.2.3.4", "\nhere be dragons"])
		fun `Parser identifies and accepts blank lines`(inputString: String) {
			val buffer = charset.encode(inputString)

			subject.parseLine(buffer)

			assertEquals(LineToken.NOTHING, subject.classify())
		}

		@Test
		fun `Attempt of reading from an exhausted chunk do not yield any token`() {
			val several = 5
			val buffer = ByteBuffer.allocate(0);

			val actualResults = List(several) {
				subject.parseLine(buffer)
			}

			val expectedResults = List(several) { false }
			assertEquals(expectedResults, actualResults)
		}

		@ParameterizedTest
		@ValueSource(strings = ["", "1", "1.2.", "1.2.3.4", "1.2.3.4.", "1.2.3.4.5", "999.0.0.0", "some other stuff"])
		fun `parseLine() consumes a single line regardless of its content`(firstLine: String) {
			val inputString = listOf(
				firstLine,
				"No one must look here".toByteArray()
			).joinToString("\n")
			val buffer = charset.encode(inputString)

			subject.parseLine(buffer)

			val expectedPosition = inputString.indexOf('\n') + 1
			assertEquals(expectedPosition, buffer.position())
		}
	}
}
