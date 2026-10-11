package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A bill with its derived review warnings. [invoiceDate] is an ISO date (`2026-10-04`). */
@Serializable
data class BillDto(
    val id: String,
    val householdId: String,
    val platform: PlatformDto,
    val invoiceNumber: String,
    val orderId: String? = null,
    val invoiceDate: String,
    val payerId: String,
    val createdBy: String,
    val status: BillStatusDto,
    val confidence: ImportConfidenceDto,
    val lines: List<BillLineDto>,
    val charges: List<BillChargeDto>,
    val totalPaise: Long,
    val warnings: List<BillWarningDto>,
)
