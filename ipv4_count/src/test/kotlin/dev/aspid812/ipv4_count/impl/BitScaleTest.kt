package dev.aspid812.ipv4_count.impl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*


class BitScaleTest {

	lateinit var subject: BitScale

	@Test
	fun `BitScale-count() equals to the number of unique witnesses`() {
		subject = BitScale(10)

		val data = arrayOf<Long>(3, 1, 4, 1, 5, 9, 2)
		data.forEach {
			subject.witness(it)
		}

		val expectedCount = setOf(data).size.toLong()
		assertEquals(expectedCount, subject.count())
	}
}