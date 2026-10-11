package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Quantity

/** Reads a typed amount such as `250`, `1.5` or `1,5` as an exact [Quantity]; null when it is not a positive amount. */
object QuantityInput {
    private val NUMBER = Regex("""^(\d{1,15})(?:[.,](\d+))?$""")

    fun parse(text: String, unit: InputUnit): Quantity? {
        val (whole, fraction) = NUMBER.matchEntire(text.trim())?.destructured ?: return null
        val extra = fraction.drop(unit.fractionDigits)
        val kept = fraction.take(unit.fractionDigits).padEnd(unit.fractionDigits, '0')
        val amount = "$whole$kept".toLongOrNull()
        return if (extra.any { it != '0' } || amount == null || amount == 0L) null else Quantity(amount, unit.base)
    }
}
