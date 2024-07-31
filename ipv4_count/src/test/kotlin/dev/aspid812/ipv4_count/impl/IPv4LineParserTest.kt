package dev.aspid812.ipv4_count.impl

import java.nio.CharBuffer

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.*
import org.junit.jupiter.params.ParameterizedTest
import org.mockito.kotlin.*


class IPv4LineParserTest {

	sealed interface IPv4Line {
		data class Address(val address: Int) : IPv4Line
		data object Mistake: IPv4Line
		data object Nothing: IPv4Line

		companion object Factory : IPv4LineParser.Visitor<IPv4Line> {
			override fun address(address: Int) = Address(address)
			override fun mistake(message: String) = Mistake     // Drop `message` for the sake of simple compare
			override fun nothing() = Nothing
		}

//		// Builder instance (with fluent syntax) is also possible
//		class Builder : IPv4LineVisitor<Builder> {
//			fun build(): IPv4Line = ...
//			override fun address(address: Int) = apply { ... }
//			... et cetera ...
//		}
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

		@ParameterizedTest
		@MethodSource("dev.aspid812.ipv4_count.impl.IPv4LineParserTest#inputLines")
		fun `visitLine() returns the same object as visitor`(line: String) {
			val cannedResult = Any()
			val visitor = mock<IPv4LineParser.Visitor<*>> {
				on { address(any()) } doReturn cannedResult
				on { mistake(any()) } doReturn cannedResult
				on { nothing() }      doReturn cannedResult
			}

			val actualResult = subject.parseLine(CharBuffer.wrap(line)).visitLine(visitor)

			assertSame(cannedResult, actualResult)
		}

		@ParameterizedTest
		@MethodSource("dev.aspid812.ipv4_count.impl.IPv4LineParserTest#inputLines")
		fun `Factory can implement a Visitor interface as well`(line: String, expectedResult: Any) {
			val lineBuffer = CharBuffer.wrap(line)

			val actualResult = subject.parseLine(lineBuffer).visitLine(IPv4Line.Factory)

			assertEquals(expectedResult, actualResult)
		}

		@Test
		//TODO: More test examples
		fun `Input string may be read by pieces`() {
			val stringPieces = "3.14.\t159.26".split("\t")

			val actualResults = listOf(
				subject.parseLine(CharBuffer.wrap(stringPieces[0])).visitLine(IPv4Line.Factory),
				subject.parseLine(CharBuffer.wrap(stringPieces[1])).visitLine(IPv4Line.Factory),
				subject.parseLine(CharBuffer.wrap("\n")).visitLine(IPv4Line.Factory),
			)

			val expectedAddress = IPv4Address.parseInt(stringPieces.joinToString(""))
			val expectedResults = listOf(null, null, IPv4Line.Address(expectedAddress))
			assertEquals(expectedResults, actualResults)
		}
	}

	@Nested
	@DisplayName("IPv4 syntax support")
	inner class IPv4Syntax {

		lateinit var visitor: IPv4LineParser.Visitor<*>

		@BeforeEach
		fun setup() {
			visitor = mock()
		}

		// „Три есть цифирь, до коей счесть потребно, и сочтенья твои суть три. До четырёх счесть не моги,
		// паче же до двух, опричь токмо коли два предшествует трём“.
		@ParameterizedTest
		@ValueSource(strings = ["1", "1.2.3", "1.2.3.4.5"])
		fun `IPv4 address consist of exactly four octets`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer).visitLine(visitor)

			verify(visitor) {
				1.times { mistake(any()) }
				0.times { address(any()) }
				0.times { nothing() }
			}
		}

		@ParameterizedTest
		@ValueSource(strings = ["256.2.3.4", "1.256.3.4", "1.2.3.256"])
		fun `Octet is a non-negative integer not greater then 255`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer).visitLine(visitor)

			verify(visitor) {
				1.times { mistake(any()) }
				0.times { address(any()) }
				0.times { nothing() }
			}
		}

		@ParameterizedTest
		@ValueSource(strings = ["00.01.02.03", "000.001.002.003", "0000.0001.0002.0003"])
		fun `Octet may start with one or more zeros`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer).visitLine(visitor)

			val expectedAddress = IPv4Address.parseInt(addressString)
			val actualAddress = argumentCaptor<Int>()
			verify(visitor) {
				1.times { address(actualAddress.capture()) }
				0.times { mistake(any()) }
				0.times { nothing() }
			}
			assertEquals(expectedAddress, actualAddress.lastValue)
		}

		@ParameterizedTest
		@ValueSource(strings = [".2.3.4", ".1.2.3.4", "1..3.4", "1.2.3.", "1.2.3.4."])
		fun `Empty octets are not allowed`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer).visitLine(visitor)

			verify(visitor) {
				1.times { mistake(any()) }
				0.times { address(any()) }
				0.times { nothing() }
			}
		}

		@ParameterizedTest
		@ValueSource(strings = ["3.14.159.26", "0.0.0.0", "1.2.3.4", "255.255.255.255"])
		fun `Examples of a well-formed IPv4 address`(addressString: String) {
			val buffer = CharBuffer.wrap("$addressString\n")

			subject.parseLine(buffer).visitLine(visitor)

			val expectedAddress = IPv4Address.parseInt(addressString)
			val actualAddress = argumentCaptor<Int>()
			verify(visitor) {
				1.times { address(actualAddress.capture()) }
				0.times { mistake(any()) }
				0.times { nothing() }
			}
			assertEquals(expectedAddress, actualAddress.lastValue)
		}
	}

	@Nested
	@DisplayName("Multi-line input handling")
	inner class MultiLineInput {

		@ParameterizedTest
		@ValueSource(strings = ["\n", "\n1.2.3.4", "\nhere be dragons"])
		fun `Parser identifies and accepts blank lines`(inputString: String) {
			val visitor = mock<IPv4LineParser.Visitor<*>>()
			val buffer = CharBuffer.wrap(inputString)

			subject.parseLine(buffer).visitLine(visitor)

			verify(visitor) {
				1.times { nothing() }
				0.times { address(any()) }
				0.times { mistake(any()) }
			}
		}

		@ParameterizedTest(name = "eof = {0}")
		@ValueSource(booleans = [true, false])
		fun `Attempt of reading from an exhausted chunk yields a null token`(eof: Boolean) {
			val several = 5
			val visitor = mock<IPv4LineParser.Visitor<*>>(defaultAnswer = { fail("Entered the wrong door") })
			val buffer = CharBuffer.allocate(0);

			val actualResults = List(several) {
				subject.parseLine(buffer).visitLine(visitor)
			}

			val expectedResults = List(several) { null }
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

			assertEquals(firstLine.length + 1, buffer.position())
		}
	}
}
