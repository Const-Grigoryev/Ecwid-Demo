package dev.aspid812.ipv4_count.impl

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*


class IPv4AddressTest {

	@Test
	fun `parseInt() parses well-formed IP address`() {
		assertEquals(0x7F000001,         IPv4Address.parseInt("127.0.0.1"))
		assertEquals(0xAC10FE01.toInt(), IPv4Address.parseInt("172.16.254.1"))
		assertEquals(0x08666768,         IPv4Address.parseInt("08.102.103.104"))
	}

	@Test
	fun `toString() produces well-formed IP address`() {
		assertEquals("127.0.0.1",      IPv4Address.toString(0x7F000001))
		assertEquals("172.16.254.1",   IPv4Address.toString(0xAC10FE01.toInt()))
		assertEquals("8.102.103.104",  IPv4Address.toString(0x08666768))
	}
}
