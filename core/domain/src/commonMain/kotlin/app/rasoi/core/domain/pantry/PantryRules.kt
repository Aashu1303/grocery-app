package app.rasoi.core.domain.pantry

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import kotlinx.datetime.LocalDate

/** How bill lines become pantry items, and what a person may change about one. */
object PantryRules {
    const val MAX_NAME_LENGTH = 120

    /** Pack size or weight times the count when the invoice gives one, otherwise the count. */
    fun quantityFor(line: BillLine): Quantity {
        val measure = line.measure ?: return Quantity.count(line.units.toLong())
        return Quantity(measure.amount * line.units, measure.unit)
    }

    fun fromLine(line: BillLine, bill: Bill, locationId: LocationId?, expiresOn: LocalDate?, id: PantryItemId) =
        PantryItem(
            id = id,
            householdId = bill.householdId,
            name = line.displayName,
            category = line.category,
            quantity = quantityFor(line),
            locationId = locationId,
            expiresOn = expiresOn,
            status = PantryStatus.IN_STOCK,
            addedOn = bill.invoiceDate,
            price = line.amount,
            payerId = bill.payerId,
            sourceLineId = line.id,
        )

    fun validate(item: NewPantryItem): AppResult<NewPantryItem> {
        val violations = nameViolations(item.name) + quantityViolations(item.quantity)
        return if (violations.isEmpty()) AppResult.Success(item) else AppResult.Failure(AppError.Validation(violations))
    }

    /** The validation error for [edit], or null when it can be applied. */
    fun problems(edit: PantryEdit): AppError.Validation? {
        val violations = buildList {
            if (edit == PantryEdit()) add(AppError.FieldViolation("body", "empty"))
            edit.name?.let { addAll(nameViolations(it)) }
            edit.quantity?.let { addAll(quantityViolations(it)) }
            if (edit.clearExpiry && edit.expiresOn != null) add(AppError.FieldViolation("expiresOn", "conflict"))
        }
        return if (violations.isEmpty()) null else AppError.Validation(violations)
    }

    fun apply(item: PantryItem, edit: PantryEdit): AppResult<PantryItem> {
        problems(edit)?.let { return AppResult.Failure(it) }
        return AppResult.Success(
            item.copy(
                name = edit.name?.trim() ?: item.name,
                quantity = edit.quantity ?: item.quantity,
                locationId = edit.locationId ?: item.locationId,
                expiresOn = if (edit.clearExpiry) null else edit.expiresOn ?: item.expiresOn,
                status = edit.status ?: item.status,
            ),
        )
    }

    private fun nameViolations(name: String): List<AppError.FieldViolation> = when {
        name.isBlank() -> listOf(AppError.FieldViolation("name", "required"))
        name.trim().length > MAX_NAME_LENGTH -> listOf(AppError.FieldViolation("name", "too_long"))
        else -> emptyList()
    }

    private fun quantityViolations(quantity: Quantity): List<AppError.FieldViolation> =
        if (quantity.isZero) listOf(AppError.FieldViolation("quantity", "required")) else emptyList()
}
