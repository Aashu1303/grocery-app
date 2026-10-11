package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A charge after editing; [id] is null for a new charge. */
@Serializable
data class BillChargeEditDto(val id: String? = null, val kind: ChargeKindDto, val label: String, val amountPaise: Long)
