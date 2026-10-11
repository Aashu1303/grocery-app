package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A row of the household's bill list. [invoiceDate] is an ISO date; [title] is set on hand-typed bills. */
@Serializable
data class BillSummaryDto(
    val id: String,
    val platform: PlatformDto,
    val title: String? = null,
    val invoiceDate: String,
    val payerId: String,
    val createdBy: String,
    val status: BillStatusDto,
    val totalPaise: Long,
    val lineCount: Int,
)
