package app.rasoi.core.domain.pantry

import app.rasoi.core.model.QuantityUnit

/**
 * The units a person types a quantity in. [fractionDigits] is how many decimals the base unit can hold for it, so
 * `1.5 kg` is exactly 1,500,000 mg and `0.5 pc` is exactly 500 thousandths.
 */
enum class InputUnit(val base: QuantityUnit, val fractionDigits: Int) {
    PIECES(QuantityUnit.COUNT, 3),
    GRAMS(QuantityUnit.MILLIGRAM, 3),
    KILOGRAMS(QuantityUnit.MILLIGRAM, 6),
    MILLILITRES(QuantityUnit.MILLILITRE, 0),
    LITRES(QuantityUnit.MILLILITRE, 3),
}
