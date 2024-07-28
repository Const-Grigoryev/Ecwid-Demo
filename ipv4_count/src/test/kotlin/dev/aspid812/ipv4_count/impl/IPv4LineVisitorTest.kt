package dev.aspid812.ipv4_count.impl

import java.io.Reader
import java.nio.CharBuffer
import java.util.function.IntSupplier

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.*
import org.junit.jupiter.params.ParameterizedTest
import org.mockito.kotlin.*

import dev.aspid812.ipv4_count.util.reader


class IPv4LineVisitorTest {

	sealed interface IPv4Line {
		data class Address(val address: Int) : IPv4Line
		data object Mistake: IPv4Line
		data object Nothing: IPv4Line

		companion object Factory : IPv4LineVisitor<IPv4Line> {
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
		fun inputLines() = samples.map { Arguments.of(it.first, it.second) }
	}

	lateinit var subject: IPv4LineVisitor<*>

	@Nested
	@DisplayName("API features")
	inner class ApiFeatures {

		@ParameterizedTest
		@MethodSource("dev.aspid812.ipv4_count.impl.IPv4LineVisitorTest#inputLines")
		fun `parseLine() returns the same object as its delegate`(addressString: String) {
			val cannedResult = Any()
			subject = spy {
				on { address(any()) } doReturn cannedResult
				on { mistake(any()) } doReturn cannedResult
				on { nothing() }      doReturn cannedResult
			}

			val actualResult = addressString.reader().use { input ->
				subject.parseLine(input)
			}

			assertSame(cannedResult, actualResult)
		}

		@ParameterizedTest
		@MethodSource("dev.aspid812.ipv4_count.impl.IPv4LineVisitorTest#inputLines")
		fun `Factory can implement a Visitor interface as well`(addressString: String, expectedResult: Any) {
			subject = IPv4Line.Factory

			val actualResult = addressString.reader().use { input ->
				subject.parseLine(input)
			}

			assertEquals(expectedResult, actualResult)
		}

		@Test
		fun `Input string may be read by pieces`() {
			val stringPieces = "3.14.\t159.26".split("\t")
			val parser = IPv4LineVisitor.Parser()
			subject = IPv4Line.Factory

			val actual = stringPieces.mapIndexed { i, piece ->
				subject.parseLine(CharBuffer.wrap(piece), parser, i == stringPieces.lastIndex)
			}

			val expectedAddress = IPv4Address.parseInt(stringPieces.joinToString(""))
			val expected = listOf(null, IPv4Line.Address(expectedAddress))
			assertEquals(expected, actual)
		}
	}

	@Nested
	@DisplayName("IPv4 syntax support")
	inner class IPv4Syntax {

		@BeforeEach
		fun setup() {
			subject = spy()
		}

		// „Три есть цифирь, до коей счесть потребно, и сочтенья твои суть три. До четырёх счесть не моги,
		// паче же до двух, опричь токмо коли два предшествует трём“.
		@ParameterizedTest
		@ValueSource(strings = ["1", "1.2.3", "1.2.3.4.5"])
		fun `IPv4 address consist of exactly four octets`(addressString: String) {
			addressString.reader().use { input ->
				subject.parseLine(input)
			}

			verify(subject) {
				1.times { mistake(any()) }
				0.times { address(any()) }
				0.times { nothing() }
			}
		}

		@ParameterizedTest
		@ValueSource(strings = ["256.2.3.4", "1.256.3.4", "1.2.3.256"])
		fun `Octet is a non-negative integer not greater then 255`(addressString: String) {
			addressString.reader().use { input ->
				subject.parseLine(input)
			}

			verify(subject) {
				1.times { mistake(any()) }
				0.times { address(any()) }
				0.times { nothing() }
			}
		}

		@ParameterizedTest
		@ValueSource(strings = ["00.01.02.03", "000.001.002.003", "0000.0001.0002.0003"])
		fun `Octet may start with one or more zeros`(addressString: String) {
			addressString.reader().use { input ->
				subject.parseLine(input)
			}

			val expectedAddress = IPv4Address.parseInt(addressString)
			val actualAddress = argumentCaptor<Int>()
			verify(subject) {
				1.times { address(actualAddress.capture()) }
				0.times { mistake(any()) }
				0.times { nothing() }
			}
			assertEquals(expectedAddress, actualAddress.lastValue)
		}

		@ParameterizedTest
		@ValueSource(strings = [".2.3.4", ".1.2.3.4", "1..3.4", "1.2.3.", "1.2.3.4."])
		fun `Empty octets are not allowed`(addressString: String) {
			addressString.reader().use { input ->
				subject.parseLine(input)
			}

			verify(subject) {
				1.times { mistake(any()) }
				0.times { address(any()) }
				0.times { nothing() }
			}
		}

		@ParameterizedTest
		@ValueSource(strings = ["3.14.159.26", "0.0.0.0", "1.2.3.4", "255.255.255.255"])
		fun `Examples of a well-formed IPv4 address`(addressString: String) {
			addressString.reader().use { input ->
				subject.parseLine(input)
			}

			val expectedAddress = IPv4Address.parseInt(addressString)
			val actualAddress = argumentCaptor<Int>()
			verify(subject) {
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

		@BeforeEach
		fun setup() {
			subject = spy()
		}

		@ParameterizedTest
		@ValueSource(strings = ["", "\n", "\n1.2.3.4", "\nhere be dragons"])
		fun `Parser identifies and accepts blank lines`(inputString: String) {
			inputString.reader().use { input ->
				subject.parseLine(input)
			}

			verify(subject) {
				1.times { nothing() }
				0.times { address(any()) }
				0.times { mistake(any()) }
			}
		}

		@Test
		fun `Reading beyond end-of-file constantly yields a specific token`() {
			val several = 5

			Reader.nullReader().use { input ->
				repeat(several) {
					subject.parseLine(input)
				}
			}

			verify(subject) {
				several.times { nothing() }
			}
		}

		@ParameterizedTest
		@ValueSource(strings = ["", "1", "1.2.", "1.2.3.4", "1.2.3.4.", "1.2.3.4.5", "999.0.0.0", "some other stuff"])
		fun `parseLine() consumes a single line regardless of its content`(firstLine: String) {
			fun inputIterator() = iterator {
				yieldAll(firstLine.iterator())
				yield('\n')
				fail("The parser went too far")
			}

			inputIterator().reader().use { input ->
				subject.parseLine(input)
			}
		}
	}
}
