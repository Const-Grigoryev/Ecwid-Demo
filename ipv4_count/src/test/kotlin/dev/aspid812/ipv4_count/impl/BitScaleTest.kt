package dev.aspid812.ipv4_count.impl

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.ValueSource
import java.util.*
import kotlin.math.*


class BitScaleTest {

	companion object {
		const val RANDOM_SEED = 556L
		const val SAMPLE_SIZE = 64_000L
	}

	lateinit var subject: BitScale

	@Test
	fun `count() returns the number of unique witnesses`() {
		val sample = arrayOf<Long>(3, 1, 4, 1, 5, 9, 2)

		subject = BitScale(10)
		sample.forEach {
			subject.witness(it)
		}

		val expectedCount = sample.distinct().count().toLong()
		assertEquals(expectedCount, subject.count())
	}

	@ValueSource(longs = [64_000L, 0x1_0000_0000])
	@ParameterizedTest
	fun `BitScale manages a large space`(spaceSize: Long) {
		val rng = Random(RANDOM_SEED)
		subject = BitScale(spaceSize)

		rng.longs(SAMPLE_SIZE, 0L, spaceSize).forEach {
			subject.witness(it)
		}

		// A pinch of statistical magic. The "rate" below means occupancy rate, i.e. the portion of BitScale's space
		// which is occupied by some sample. Since we have used `Random` class here, it is a random value. See Poisson
		// distribution, central limit theorem and the three-sigma law for a comprehensive reference.
		val actualRate = subject.count() / spaceSize.toDouble()
		val rateCeiling = SAMPLE_SIZE / spaceSize.toDouble()
		val rateExpectation = -expm1(-rateCeiling)
		val rateDeviation = sqrt(rateExpectation * (1 - rateExpectation) / spaceSize)
		assertTrue(actualRate > rateExpectation - 3 * rateDeviation)    // With probability > 99.7%
	}
}