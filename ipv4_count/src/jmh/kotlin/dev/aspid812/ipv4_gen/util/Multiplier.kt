package dev.aspid812.ipv4_gen.util


enum class Multiplier(
	val numericValue: Long,
	val abbreviation: String
) {
	THOUSAND(1_000L, "K"),
	MILLION(1_000_000L, "M"),
	BILLION(1_000_000_000L, "B"),
	TRILLION(1_000_000_000_000L, "T");

	companion object {
		val abbreviationMap = entries.associateBy(Multiplier::abbreviation)

		@JvmStatic
		fun parseLong(numeral: String): Long {
			// Extremely wasteful solution, but instead it looks pretty good!
			val (suffix, multiplier) = abbreviationMap
				.mapValues { it.value.numericValue }
				.filterKeys(numeral::endsWith)
				.ifEmpty { mapOf("" to 1L) }
				.entries.single()
			return numeral.removeSuffix(suffix).toLong() * multiplier
		}
	}
}

// Nice, but useless for now
//fun Multiplier.toLong() = this.numericValue
//fun Multiplier.toDouble() = this.numericValue.toDouble()
//fun Multiplier.toBigInteger() = BigInteger.valueOf(this.numericValue)
//fun Multiplier.toBigDecimal() = BigDecimal.valueOf(this.numericValue)
