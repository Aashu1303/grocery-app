package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** Replaces the lines, charges, printed total, payer or splits of a bill; omitted parts stay unchanged. */
@Serializable
data class UpdateBillRequestDto(
    val lines: List<BillLineEditDto>? = null,
    val charges: List<BillChargeEditDto>? = null,
    val totalPaise: Long? = null,
    val payerId: String? = null,
    val splits: List<LineSplitEditDto>? = null,
)
