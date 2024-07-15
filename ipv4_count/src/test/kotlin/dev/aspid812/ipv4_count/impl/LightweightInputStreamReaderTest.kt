package dev.aspid812.ipv4_count.impl

import java.io.LineNumberReader
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock


class LightweightInputStreamReaderTest {

	companion object {
		const val NEWLINE = '\n'.code
		const val NO_CHAR = -1
	}

	lateinit var subject: LightweightInputStreamReader

	@Test
	fun `decode() normalizes newline sequences`() {
		subject = LightweightInputStreamReader(mock())

		assertEquals(NEWLINE, subject.decode(0x1111110D, NO_CHAR))
		assertEquals(NEWLINE, subject.decode(0x1111110A, NO_CHAR))
		assertEquals(NO_CHAR, subject.decode(0x11110D0A, NO_CHAR))
		assertEquals(NEWLINE, subject.decode(0x11110A0D, NO_CHAR))
		assertEquals(0x22, subject.decode(0x11110D22, NO_CHAR))
		assertEquals(0x22, subject.decode(0x11110A22, NO_CHAR))
		assertEquals(0x22, subject.decode(0x110D0A22, NO_CHAR))
	}

	@Test
	fun `Overall behavior is identical to LineNumberReader`() {
		val encoding = StandardCharsets.US_ASCII
		val sampleText = listOf(
				"Beware the Jabberwock, my son!\n",
				"The jaws that bite, the claws that catch!\r",
				"Beware the Jubjub bird, and shun\r\n",
				"The frumious Bandersnatch!"
			)
			.joinToString("")
			.toByteArray(encoding)
		val referent = LineNumberReader(sampleText.inputStream().reader(encoding))
		val subject = LightweightInputStreamReader(sampleText.inputStream())

		val expected = referent.use { input ->
			CharArray(sampleText.size) { input.read().toChar() }
		}
		val actual = subject.use { input ->
			CharArray(sampleText.size) { input.read().toChar() }
		}

		assertArrayEquals(expected, actual)
	}
}