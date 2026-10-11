package app.rasoi.core.domain.bills

import app.rasoi.core.domain.pantry.Categorizer
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.sum

/** Reconciliation warnings and draft editing rules for bills (design-plan 6.3 and 6.5). */
object BillRules {
    const val MAX_LINES = 300
    const val MAX_NAME_LENGTH = 120
    const val MAX_UNITS = 999

    /** Warnings are derived from the bill's current state, so an edit that fixes the totals clears them. */
    fun warnings(bill: Bill): List<BillWarning> {
        val computed = (bill.lines.map { it.amount } + bill.charges.map { it.amount }).sum()
        return listOfNotNull(
            BillWarning.TotalMismatch(computed, bill.total).takeIf { computed != bill.total },
            BillWarning.ParsedByFallback.takeIf { bill.confidence == ImportConfidence.LOW },
        )
    }

    fun ensureEditable(bill: Bill): AppResult<Unit> = when (bill.status) {
        BillStatus.DRAFT -> AppResult.Success(Unit)
        else -> AppResult.Failure(AppError.Conflict("bill_not_editable"))
    }

    fun validate(edit: BillEdit): AppResult<BillEdit> {
        val violations = buildList {
            if (edit.lines == null && edit.charges == null && edit.total == null) add(violation("body", "empty"))
            edit.lines?.let { lines ->
                when {
                    lines.isEmpty() -> add(violation("lines", "required"))
                    lines.size > MAX_LINES -> add(violation("lines", "too_many"))
                    else -> lines.forEachIndexed { index, line -> addAll(lineViolations("lines[$index]", line)) }
                }
            }
            edit.charges?.let { charges ->
                if (charges.size > MAX_LINES) add(violation("charges", "too_many"))
                charges.forEachIndexed { index, charge ->
                    addAll(nameViolations("charges[$index].label", charge.label))
                }
            }
            if (edit.total?.isNegative == true) add(violation("total", "negative"))
        }
        return if (violations.isEmpty()) AppResult.Success(edit) else AppResult.Failure(AppError.Validation(violations))
    }

    /** Applies a validated [edit]. Existing ids keep their raw names; new lines and charges get fresh ids. */
    fun apply(bill: Bill, edit: BillEdit, newLineId: () -> BillLineId, newChargeId: () -> ChargeId): AppResult<Bill> {
        val existingLines = bill.lines.associateBy { it.id }
        val existingCharges = bill.charges.associateBy { it.id }
        val unknown = buildList {
            edit.lines?.forEachIndexed { index, line ->
                if (line.id != null && line.id !in existingLines) add(violation("lines[$index].id", "unknown"))
            }
            edit.charges?.forEachIndexed { index, charge ->
                if (charge.id != null && charge.id !in existingCharges) add(violation("charges[$index].id", "unknown"))
            }
        }
        if (unknown.isNotEmpty()) return AppResult.Failure(AppError.Validation(unknown))
        val lines = edit.lines?.map { line -> editedLine(line, existingLines, bill, newLineId) }
        val charges = edit.charges?.map { charge ->
            BillCharge(charge.id ?: newChargeId(), charge.kind, charge.label.trim(), charge.amount)
        }
        return AppResult.Success(
            bill.copy(lines = lines ?: bill.lines, charges = charges ?: bill.charges, total = edit.total ?: bill.total),
        )
    }

    private fun editedLine(
        edit: LineEdit,
        existing: Map<BillLineId, BillLine>,
        bill: Bill,
        newLineId: () -> BillLineId,
    ): BillLine {
        val name = edit.displayName.trim()
        val current = edit.id?.let { existing.getValue(it) }
        if (current != null) {
            return current.copy(
                displayName = name,
                units = edit.units,
                amount = edit.amount,
                isFood = edit.isFood ?: current.isFood,
            )
        }
        val categorization = Categorizer.categorize(name, hsn = null, platform = bill.platform)
        return BillLine(
            id = newLineId(),
            rawName = name,
            displayName = name,
            units = edit.units,
            amount = edit.amount,
            category = categorization.category,
            isFood = edit.isFood ?: categorization.isFood,
        )
    }

    private fun lineViolations(prefix: String, line: LineEdit) = buildList {
        addAll(nameViolations("$prefix.name", line.displayName))
        if (line.units !in 1..MAX_UNITS) add(violation("$prefix.units", "out_of_range"))
        if (line.amount.isNegative) add(violation("$prefix.amount", "negative"))
    }

    private fun nameViolations(field: String, name: String) = listOfNotNull(
        when {
            name.isBlank() -> violation(field, "required")
            name.trim().length > MAX_NAME_LENGTH -> violation(field, "too_long")
            else -> null
        },
    )

    private fun violation(field: String, code: String) = AppError.FieldViolation(field, code)
}
