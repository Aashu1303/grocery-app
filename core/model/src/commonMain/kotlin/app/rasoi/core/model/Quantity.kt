package app.rasoi.core.model

/**
 * A non-negative amount in base units (ADR 0002): milligrams, millilitres, or count x 1000.
 * Arithmetic and comparison are defined only between quantities of the same [unit].
 */
data class Quantity(val amount: Long, val unit: QuantityUnit) : Comparable<Quantity> {
    init {
        require(amount >= 0) { "Quantity cannot be negative: $amount" }
    }

    val isZero: Boolean get() = amount == 0L

    operator fun plus(other: Quantity): Quantity = sameUnit(other) { copy(amount = addExact(amount, other.amount)) }

    operator fun minus(other: Quantity): Quantity =
        sameUnit(other) { copy(amount = subtractExact(amount, other.amount)) }

    override fun compareTo(other: Quantity): Int = sameUnit(other) { amount.compareTo(other.amount) }

    private inline fun <R> sameUnit(other: Quantity, block: () -> R): R {
        require(unit == other.unit) { "Unit mismatch: $unit vs ${other.unit}" }
        return block()
    }

    companion object {
        private const val GRAMS_PER_KILOGRAM = 1_000L
        private const val MILLILITRES_PER_LITRE = 1_000L

        fun zero(unit: QuantityUnit): Quantity = Quantity(0, unit)

        fun grams(grams: Long): Quantity =
            Quantity(multiplyExact(grams, QuantityUnit.MILLIGRAM.perWhole), QuantityUnit.MILLIGRAM)

        fun kilograms(kilograms: Long): Quantity = grams(multiplyExact(kilograms, GRAMS_PER_KILOGRAM))

        fun millilitres(millilitres: Long): Quantity = Quantity(millilitres, QuantityUnit.MILLILITRE)

        fun litres(litres: Long): Quantity = millilitres(multiplyExact(litres, MILLILITRES_PER_LITRE))

        fun count(pieces: Long): Quantity =
            Quantity(multiplyExact(pieces, QuantityUnit.COUNT.perWhole), QuantityUnit.COUNT)
    }
}
