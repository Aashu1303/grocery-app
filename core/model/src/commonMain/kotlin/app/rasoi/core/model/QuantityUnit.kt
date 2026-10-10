package app.rasoi.core.model

/** Base unit of a [Quantity]. Counts are stored in thousandths so fractional pieces stay exact. */
enum class QuantityUnit(val perWhole: Long) {
    MILLIGRAM(perWhole = 1_000),
    MILLILITRE(perWhole = 1),
    COUNT(perWhole = 1_000),
}
