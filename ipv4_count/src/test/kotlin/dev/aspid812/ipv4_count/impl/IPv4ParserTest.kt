package dev.aspid812.ipv4_count.impl

import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito

import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions

import java.io.StringReader

import dev.aspid812.ipv4_count.impl.IPv4Parser.ParseResult
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource


class IPv4ParserTest {

	companion object {
		private fun ipStringToInt(addressString: String): Int =
			addressString.splitToSequence('.')
				.map(Integer::parseInt)
				.fold(0) { address, octet -> address.shl(8) + octet }

		@JvmStatic
		fun invalidAddresses() = listOf(
			"Too few octets"           to "1.2.3",
			"Too many octets"          to "1.2.3.4.5",
			"Skipped octet"            to "1.2..4",
			"Octet starting with zero" to "1.2.03.4",
			"Octet is too large"       to "1.2.256.4",
		).map { Named.of(it.first, it.second) }
	}

	@Test
	fun `Test utility sanity check`() {
		assertEquals(0x7F000001,         ipStringToInt("127.0.0.1"))
		assertEquals(0xC0A8585B.toInt(), ipStringToInt("192.168.88.91"))
	}


	lateinit var subject: IPv4Parser
	lateinit var sink: IPv4Builder

	@BeforeEach
	fun setup() {
		sink = Mockito.mock(IPv4Builder::class.java)
	}

	@ParameterizedTest
	@ValueSource(strings=["127.0.0.1", "192.168.88.91"])
	fun `Correct cases`(addressString: String) {
		subject = IPv4Parser(StringReader(addressString))

		val result = subject.parseNextLine(sink)

		val expectedAddress = ipStringToInt(addressString)
		assertEquals(ParseResult.SUCCESS, result)
		verify(sink).accept(expectedAddress)
		verifyNoMoreInteractions(sink)
	}

	@ParameterizedTest
	@MethodSource("invalidAddresses")
	fun `Invalid cases`(addressString: String) {
		subject = IPv4Parser(StringReader(addressString))

		val result = subject.parseNextLine(sink)

		assertEquals(ParseResult.ERROR, result)
		verifyNoInteractions(sink)
	}


	@Test
	fun `Empty octets are not allowed`() {
		subject = IPv4Parser(StringReader("1...4"))
		subject.parseNextLine(sink)
	}
}