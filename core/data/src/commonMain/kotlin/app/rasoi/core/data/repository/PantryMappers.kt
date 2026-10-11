package app.rasoi.core.data.repository

import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.QuantityDto
import app.rasoi.core.contract.bills.QuantityUnitDto
import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.contract.pantry.PantryItemDto
import app.rasoi.core.contract.pantry.PantryStatusDto
import app.rasoi.core.contract.pantry.UpdatePantryItemRequestDto
import app.rasoi.core.domain.pantry.NewPantryItem
import app.rasoi.core.domain.pantry.PantryEdit
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.Category
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.Money
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import kotlinx.datetime.LocalDate

internal fun Quantity.toDto() = QuantityDto(amount, QuantityUnitDto.valueOf(unit.name))

internal fun QuantityDto.toModel() = Quantity(amount, QuantityUnit.valueOf(unit.name))

internal fun PantryItemDto.toModel(householdId: HouseholdId) = PantryItem(
    id = PantryItemId(id),
    householdId = householdId,
    name = name,
    category = Category.valueOf(category.name),
    quantity = quantity.toModel(),
    locationId = locationId?.let(::LocationId),
    expiresOn = expiresOn?.let(LocalDate::parse),
    status = PantryStatus.valueOf(status.name),
    addedOn = LocalDate.parse(addedOn),
    price = pricePaise?.let(::Money),
    sourceLineId = sourceLineId?.let(::BillLineId),
)

internal fun NewPantryItem.toDto() = CreatePantryItemRequestDto(
    name = name,
    category = CategoryDto.valueOf(category.name),
    quantity = quantity.toDto(),
    locationId = locationId?.value,
    expiresOn = expiresOn?.toString(),
)

internal fun PantryEdit.toDto() = UpdatePantryItemRequestDto(
    name = name,
    quantity = quantity?.toDto(),
    locationId = locationId?.value,
    expiresOn = expiresOn?.toString(),
    clearExpiry = clearExpiry,
    status = status?.let { PantryStatusDto.valueOf(it.name) },
)
