package dev.aspid812.ipv4_count.impl

import java.nio.CharBuffer

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.*
import org.junit.jupiter.params.ParameterizedTest
import dev.aspid812.ipv4_count.impl.IPv4LineParser.LineToken


class IPv4LineParserTest {

	sealed interface IPv4Line {
		data class Address(val address: Int) : IPv4Line
		data object Mistake: IPv4Line
		data object Nothing: IPv4Line
	}

	private fun IPv4LineParser.toIPv4Line() =
		when (this.classify()!!) {
			LineToken.VALID_ADDRESS -> IPv4Line.Address(this.address)
			LineToken.IRRELEVANT_CONTENT -> IPv4Line.Mistake
			LineToken.NOTHING -> IPv4Line.Nothing
		}

	companion object {
		private val samples = listOf(
			""                to IPv4Line.Nothing,
			"1"               to IPv4Line.Mistake,     // Meaningful string, but not an address
			"1.2."            to IPv4Line.Mistake,     // Sudden line brake
			"1.2..4"          to IPv4Line.Mistake,     // Empty octet place
			"1.2.3.4."        to IPv4Line.Mistake,     // Period closes the last octet
			"1.2.3.4.5"       to IPv4Line.Mistake,     // Too many octets
			"999.0.0.0"       to IPv4Line.Mistake,     // Illegal value of an octet
			"Kilroy was here" to IPv4Line.Mistake,     // Not an IP address at all
			"1.2.3.4"         to IPv4Line.Address(0x01020304),
			"1.02.003.0004"   to IPv4Line.Address(0x01020304),
		)

		@JvmStatic  // Still waiting for a more general solution...
		fun inputLines() = samples.map { Arguments.of(it.first + "\n", it.second) }
	}

	lateinit var subject: IPv4LineParser

	@BeforeEach
	fun setup() {
		subject = IPv4LineParser()
	}

	@Nested
	@DisplayName("API features")
	inner class ApiFeatures {

		@Test
		//TODO: More test examples
		fun `Input string may be read by pieces`() {
			val stringPieces = "3.14.\t159.26".split("\t")

			val actualResults = listOf(
				with(subject) { parseLine(CharBuffer.wrap(stringPieces[0])); ready() },
				with(subject) { parseLine(CharBuffer.wrap(stringPieces[1])); ready() },
				with(subject) { parseLine(CharBuffer.wrap("\n")); ready() },
			)
			val expectedResults = listOf(false, false, true)
			assertEquals(expectedResults, actualResults)

			val expectedValue = stringPieces.joinToString("")
				.let(IPv4Address::parseInt)
				.let(IPv4Line::Address)
			assertEquals(expectedValue, subject.toIPv4Line())
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
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer)

			assertEquals(LineToken.IRRELEVANT_CONTENT, subject.classify())
		}

		@ParameterizedTest
		@ValueSource(strings = ["256.2.3.4", "1.256.3.4", "1.2.3.256"])
		fun `Octet is a non-negative integer not greater then 255`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer)

			assertEquals(LineToken.IRRELEVANT_CONTENT, subject.classify())
		}

		@ParameterizedTest
		@ValueSource(strings = ["00.01.02.03", "000.001.002.003", "0000.0001.0002.0003"])
		fun `Octet may start with one or more zeros`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer)

			val expectedAddress = IPv4Address.parseInt(addressString)
			assertEquals(LineToken.VALID_ADDRESS, subject.classify())
			assertEquals(expectedAddress, subject.address)
		}

		@ParameterizedTest
		@ValueSource(strings = [".2.3.4", ".1.2.3.4", "1..3.4", "1.2.3.", "1.2.3.4."])
		fun `Empty octets are not allowed`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer)

			assertEquals(LineToken.IRRELEVANT_CONTENT, subject.classify())
		}

		@ParameterizedTest
		@ValueSource(strings = ["3.14.159.26", "0.0.0.0", "1.2.3.4", "255.255.255.255"])
		fun `Examples of a well-formed IPv4 address`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

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
			val buffer = CharBuffer.wrap(inputString)

			subject.parseLine(buffer)

			assertEquals(LineToken.NOTHING, subject.classify())
		}

		@ParameterizedTest(name = "eof = {0}")
		@ValueSource(booleans = [true, false])
		fun `Attempt of reading from an exhausted chunk fails to yield any token`(eof: Boolean) {
			val several = 5
			val buffer = CharBuffer.allocate(0);

			val actualResults = List(several) {
				subject.parseLine(buffer)
				subject.ready()
			}

			val expectedResults = List(several) { false }
			assertEquals(expectedResults, actualResults)
		}

		@ParameterizedTest
		@ValueSource(strings = ["", "1", "1.2.", "1.2.3.4", "1.2.3.4.", "1.2.3.4.5", "999.0.0.0", "some other stuff"])
		fun `parseLine() consumes a single line regardless of its content`(firstLine: String) {
			val buffer = CharBuffer.allocate(128)
				.put(firstLine)
				.put('\n')
				.put("No one must look here")
				.flip()

			subject.parseLine(buffer)

			val expectedPosition = firstLine.length + 1
			assertEquals(expectedPosition, buffer.position())
		}
	}
}
