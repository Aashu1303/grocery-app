package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A bill typed in by hand. [invoiceDate] is an ISO date; [payerId] defaults to the caller. */
@Serializable
data class CreateBillRequestDto(
    val title: String,
    val invoiceDate: String,
    val lines: List<BillLineEditDto>,
    val charges: List<BillChargeEditDto> = emptyList(),
    val payerId: String? = null,
)
