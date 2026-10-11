package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.core.model.sum
import kotlinx.datetime.LocalDate

/**
 * A bill typed in by hand (design-plan 6.3), for what no invoice covers: rent, a gas cylinder, a shared gift.
 * A null [invoiceDate] means today in the household's time zone.
 */
data class ManualBillDraft(
    val title: String,
    val invoiceDate: LocalDate? = null,
    val payerId: UserId,
    val lines: List<LineEdit>,
    val charges: List<ChargeEdit>,
)

object ManualBills {
    /** Checks the title and lines the way the server will, so the form can show field errors before sending. */
    fun validate(draft: ManualBillDraft): AppResult<ManualBillDraft> {
        val title = draft.title.trim()
        if (title.isBlank() || title.length > BillRules.MAX_NAME_LENGTH) {
            val code = if (title.isBlank()) "required" else "too_long"
            return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("title", code))))
        }
        return BillRules.validate(editOf(draft)).map { draft.copy(title = title) }
    }

    /** Builds a draft whose total is exactly its lines plus its charges, so it never carries a mismatch warning. */
    fun create(
        id: BillId,
        householdId: HouseholdId,
        creator: UserId,
        draft: ManualBillDraft,
        today: LocalDate,
        newLineId: () -> BillLineId,
        newChargeId: () -> ChargeId,
    ): AppResult<Bill> = validate(draft).flatMap { valid ->
        val empty = Bill(
            id = id,
            householdId = householdId,
            platform = Platform.MANUAL,
            invoiceNumber = "manual-${id.value.take(INVOICE_SUFFIX_LENGTH)}",
            orderId = null,
            invoiceDate = valid.invoiceDate ?: today,
            payerId = valid.payerId,
            createdBy = creator,
            status = BillStatus.DRAFT,
            confidence = ImportConfidence.HIGH,
            lines = emptyList(),
            charges = emptyList(),
            total = Money.ZERO,
            title = valid.title,
        )
        BillRules.apply(empty, editOf(valid), newLineId, newChargeId)
    }

    private fun editOf(draft: ManualBillDraft) = BillEdit(
        lines = draft.lines,
        charges = draft.charges,
        total = (draft.lines.map { it.amount } + draft.charges.map { it.amount }).sum(),
    )

    private const val INVOICE_SUFFIX_LENGTH = 8
}
