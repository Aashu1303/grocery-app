package app.rasoi.server.http

import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.QuantityDto
import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.contract.pantry.PantryItemDto
import app.rasoi.core.contract.pantry.PantryStatusDto
import app.rasoi.core.contract.pantry.UpdatePantryItemRequestDto
import app.rasoi.core.domain.pantry.NewPantryItem
import app.rasoi.core.domain.pantry.PantryEdit
import app.rasoi.core.model.Category
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import kotlinx.datetime.LocalDate

internal fun QuantityDto.toModel() = Quantity(amount, QuantityUnit.valueOf(unit.name))

internal fun PantryItem.toDto() = PantryItemDto(
    id = id.value,
    name = name,
    category = CategoryDto.valueOf(category.name),
    quantity = quantity.toDto(),
    locationId = locationId?.value,
    expiresOn = expiresOn?.toString(),
    status = PantryStatusDto.valueOf(status.name),
    addedOn = addedOn.toString(),
    pricePaise = price?.paise,
    sourceLineId = sourceLineId?.value,
)

/** Null when a date in the request cannot be read; the route reports it as a validation failure. */
internal fun CreatePantryItemRequestDto.toModel(): NewPantryItem? {
    val expiry = expiresOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return null }
    return NewPantryItem(
        name = name,
        category = Category.valueOf(category.name),
        quantity = quantity.toModel(),
        locationId = locationId?.let(::LocationId),
        expiresOn = expiry,
    )
}

internal fun UpdatePantryItemRequestDto.toModel(): PantryEdit? {
    val expiry = expiresOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return null }
    return PantryEdit(
        name = name,
        quantity = quantity?.toModel(),
        locationId = locationId?.let(::LocationId),
        expiresOn = expiry,
        clearExpiry = clearExpiry,
        status = status?.let { PantryStatus.valueOf(it.name) },
    )
}
