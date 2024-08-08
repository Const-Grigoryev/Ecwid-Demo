package dev.aspid812.ipv4_count

import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.div

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test


class IPv4CountAppTest {

	companion object {
		const val TEMP_FILE_PREFIX = "IPv4Count."
		const val TEMP_FILE_SUFFIX = ".tmp"

		val tempDir = Path(System.getProperty("java.io.tmpdir"))
	}

	@Test
	fun `Application may accept several input files`() {
		val file = Files.createTempFile(tempDir, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)

		val application = IPv4CountApp.forCommandLine("$file", "$file")

		assertEquals(IPv4CountApp::class.java, application.javaClass)
		if (application is IPv4CountApp) {
			assertEquals(listOf(file, file), application.inputFiles)
			assertEquals(false, application.includeStandardInput)
		}
	}

	@Test
	fun `Application switches to STDIO unless is has no input files given`() {
		val application = IPv4CountApp.forCommandLine()

		assertEquals(IPv4CountApp::class.java, application.javaClass)
		if (application is IPv4CountApp) {
			assertEquals(true, application.includeStandardInput)
		}
	}

	@Test
	fun `Application denies to accept a non-existent file`() {
		val file = tempDir / "$TEMP_FILE_PREFIX(non-existent)$TEMP_FILE_SUFFIX"

		val application = IPv4CountApp.forCommandLine("$file")

		assertNotEquals(IPv4CountApp::class.java, application.javaClass)
	}

	@Test
	fun `Application fails to startup whenever it encounters a wrong syntax of the file path`() {
		val application = IPv4CountApp.forCommandLine("$tempDir/*.txt")

		assertNotEquals(IPv4CountApp::class.java, application.javaClass)
	}
}
