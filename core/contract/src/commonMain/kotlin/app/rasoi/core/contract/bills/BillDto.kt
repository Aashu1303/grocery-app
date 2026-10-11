package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A bill with its derived review warnings and, once split, what each member owes. [invoiceDate] is an ISO date
 * (`2026-10-04`); [title] is set only on bills typed in by hand.
 */
@Serializable
data class BillDto(
    val id: String,
    val householdId: String,
    val platform: PlatformDto,
    val invoiceNumber: String,
    val orderId: String? = null,
    val title: String? = null,
    val invoiceDate: String,
    val payerId: String,
    val createdBy: String,
    val status: BillStatusDto,
    val confidence: ImportConfidenceDto,
    val lines: List<BillLineDto>,
    val charges: List<BillChargeDto>,
    val totalPaise: Long,
    val warnings: List<BillWarningDto>,
    val allocations: List<AllocationDto> = emptyList(),
    val approvals: List<ApprovalDto> = emptyList(),
    val shares: List<MemberShareDto> = emptyList(),
    val submittedAt: Instant? = null,
)
