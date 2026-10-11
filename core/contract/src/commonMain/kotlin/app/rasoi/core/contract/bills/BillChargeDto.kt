package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A bill-level charge; discounts are negative. */
@Serializable
data class BillChargeDto(
    val id: String,
    val kind: ChargeKindDto,
    val label: String,
    val amountPaise: Long,
    val distribution: ChargeDistributionDto = ChargeDistributionDto.PROPORTIONAL,
)
