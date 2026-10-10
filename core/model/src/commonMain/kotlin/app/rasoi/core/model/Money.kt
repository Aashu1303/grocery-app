package app.rasoi.core.model

import kotlin.jvm.JvmInline

/**
 * An amount of Indian rupees held as whole paise (ADR 0002).
 *
 * Arithmetic is exact and fails with [ArithmeticException] on overflow instead of wrapping.
 */
@JvmInline
value class Money(val paise: Long) : Comparable<Money> {
    val isZero: Boolean get() = paise == 0L
    val isNegative: Boolean get() = paise < 0L
    val absoluteValue: Money get() = if (isNegative) -this else this

    operator fun plus(other: Money): Money = Money(addExact(paise, other.paise))

    operator fun minus(other: Money): Money = Money(subtractExact(paise, other.paise))

    operator fun unaryMinus(): Money = Money(subtractExact(0L, paise))

    operator fun times(factor: Long): Money = Money(multiplyExact(paise, factor))

    override fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    /** Formats as rupees with Indian digit grouping, for example `₹1,23,456.00` or `-₹12.50`. */
    fun format(): String {
        val rupees = paise / PAISE_PER_RUPEE
        val fraction = (paise % PAISE_PER_RUPEE).let { if (it < 0) -it else it }
        val digits = (if (rupees < 0) -rupees else rupees).toString()
        val sign = if (paise < 0) "-" else ""
        return "$sign$RUPEE${groupIndian(digits)}.${fraction.toString().padStart(2, '0')}"
    }

    override fun toString(): String = format()

    companion object {
        val ZERO = Money(0)
        private const val PAISE_PER_RUPEE = 100L
        private const val RUPEE = "₹"
        private const val THOUSANDS_GROUP = 3
        private const val LAKH_GROUP = 2
        private val DECIMAL = Regex("""^(-)?(\d{1,2}(?:,\d{2})*,\d{3}|\d{1,3}(?:,\d{3})+|\d+)?(?:\.(\d{1,2}))?$""")

        fun ofRupees(rupees: Long): Money = Money(multiplyExact(rupees, PAISE_PER_RUPEE))

        /**
         * Parses a plain decimal amount such as `469.01`, `-1.0` or `1,23,456.78` without floating point.
         * Returns null for anything else, including more than two decimal places or values out of range.
         */
        fun parse(text: String): Money? {
            val (sign, whole, fraction) = DECIMAL.matchEntire(text)?.destructured ?: return null
            if (whole.isEmpty() && fraction.isEmpty()) return null
            return runCatching {
                val rupees = whole.replace(",", "").ifEmpty { "0" }.toLong()
                val magnitude = addExact(multiplyExact(rupees, PAISE_PER_RUPEE), fraction.padEnd(2, '0').toLong())
                Money(if (sign.isEmpty()) magnitude else -magnitude)
            }.getOrNull()
        }

        private fun groupIndian(digits: String): String {
            if (digits.length <= THOUSANDS_GROUP) return digits
            val head = digits.dropLast(THOUSANDS_GROUP)
            val pairs = head.reversed().chunked(LAKH_GROUP).joinToString(",").reversed()
            return "$pairs,${digits.takeLast(THOUSANDS_GROUP)}"
        }
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO, Money::plus)
