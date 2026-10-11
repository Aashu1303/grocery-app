package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** Replaces the lines, charges or printed total of a draft; omitted parts stay unchanged. */
@Serializable
data class UpdateBillRequestDto(
    val lines: List<BillLineEditDto>? = null,
    val charges: List<BillChargeEditDto>? = null,
    val totalPaise: Long? = null,
)
