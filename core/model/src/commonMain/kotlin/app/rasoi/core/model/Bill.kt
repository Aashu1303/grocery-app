package app.rasoi.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * A household bill built from one or more invoices of one order, or entered by hand (design-plan 6.2). [version]
 * counts saves so two people changing the bill at once cannot overwrite each other.
 */
data class Bill(
    val id: BillId,
    val householdId: HouseholdId,
    val platform: Platform,
    val invoiceNumber: String,
    val orderId: String?,
    val invoiceDate: LocalDate,
    val payerId: UserId,
    val createdBy: UserId,
    val status: BillStatus,
    val confidence: ImportConfidence,
    val lines: List<BillLine>,
    val charges: List<BillCharge>,
    val total: Money,
    val allocations: List<Allocation> = emptyList(),
    val approvals: List<Approval> = emptyList(),
    val submittedAt: Instant? = null,
    val title: String? = null,
    val version: Int = 0,
)
