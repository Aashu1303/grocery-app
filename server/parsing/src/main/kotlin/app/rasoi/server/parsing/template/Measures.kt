package app.rasoi.server.parsing.template

import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit

private val DECIMAL = Regex("""^(\d{1,9})(?:\.(\d{1,3}))?$""")
private const val FRACTION_DIGITS = 3
private const val THOUSAND = 1_000L

/** A printed measure such as `470.00 g`, `1.5 kg`, `500 ml` or `2 pc`; null when it is not one. */
internal fun parseMeasure(number: String, unit: String): Quantity? {
    val (whole, fraction) = DECIMAL.matchEntire(number)?.destructured ?: return null
    val thousandths = whole.toLong() * THOUSAND + fraction.padEnd(FRACTION_DIGITS, '0').toLong()
    return when (unit.lowercase().trimEnd('.')) {
        "g", "gm", "gms" -> Quantity(thousandths, QuantityUnit.MILLIGRAM)
        "kg", "kgs" -> Quantity(thousandths * THOUSAND, QuantityUnit.MILLIGRAM)
        "ml" -> Quantity(thousandths / THOUSAND, QuantityUnit.MILLILITRE)
        "l", "ltr" -> Quantity(thousandths, QuantityUnit.MILLILITRE)
        "pc", "pcs", "piece", "pieces" -> Quantity(thousandths, QuantityUnit.COUNT)
        else -> null
    }
}
