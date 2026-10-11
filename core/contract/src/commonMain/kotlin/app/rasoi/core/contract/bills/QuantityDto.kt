package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A quantity in base units: milligrams, millilitres or thousandths of a piece. */
@Serializable
data class QuantityDto(val amount: Long, val unit: QuantityUnitDto)
