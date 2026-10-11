package app.rasoi.core.model

private const val MILLIGRAMS_PER_GRAM = 1_000L
private const val MILLIGRAMS_PER_KILOGRAM = 1_000_000L
private const val MILLILITRES_PER_LITRE = 1_000L
private const val THOUSANDTHS = 1_000L
private const val GRAM_DIGITS = 3
private const val KILOGRAM_DIGITS = 6

/** A quantity as written on invoices: `470 g`, `1.5 kg`, `500 ml`, `1.5 L`, `6 pc`. Unit symbols are not translated. */
fun Quantity.format(): String = when (unit) {
    QuantityUnit.MILLIGRAM ->
        if (amount < MILLIGRAMS_PER_KILOGRAM) {
            "${decimal(amount, MILLIGRAMS_PER_GRAM, GRAM_DIGITS)} g"
        } else {
            "${decimal(amount, MILLIGRAMS_PER_KILOGRAM, KILOGRAM_DIGITS)} kg"
        }
    QuantityUnit.MILLILITRE ->
        if (amount < MILLILITRES_PER_LITRE) "$amount ml" else "${decimal(amount, MILLILITRES_PER_LITRE, GRAM_DIGITS)} L"
    QuantityUnit.COUNT -> "${decimal(amount, THOUSANDTHS, GRAM_DIGITS)} pc"
}

private fun decimal(value: Long, perWhole: Long, digits: Int): String {
    val fraction = (value % perWhole).toString().padStart(digits, '0').trimEnd('0')
    return if (fraction.isEmpty()) "${value / perWhole}" else "${value / perWhole}.$fraction"
}
