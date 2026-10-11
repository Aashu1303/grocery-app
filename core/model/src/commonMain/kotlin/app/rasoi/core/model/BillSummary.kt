package app.rasoi.core.model

import kotlinx.datetime.LocalDate

/**
 * A row of the bill list: enough to show and open a bill without loading its lines. [title] is set on hand-typed
 * bills.
 */
data class BillSummary(
    val id: BillId,
    val platform: Platform,
    val title: String?,
    val invoiceDate: LocalDate,
    val payerId: UserId,
    val createdBy: UserId,
    val status: BillStatus,
    val total: Money,
    val lineCount: Int,
)
