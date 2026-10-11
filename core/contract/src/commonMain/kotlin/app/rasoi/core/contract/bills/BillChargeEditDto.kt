package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A charge after editing; [id] is null for a new charge and a null [distribution] keeps the current one. */
@Serializable
data class BillChargeEditDto(
    val id: String? = null,
    val kind: ChargeKindDto,
    val label: String,
    val amountPaise: Long,
    val distribution: ChargeDistributionDto? = null,
)
