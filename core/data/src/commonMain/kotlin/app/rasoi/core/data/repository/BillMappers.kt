package app.rasoi.core.data.repository

import app.rasoi.core.contract.bills.BillChargeDto
import app.rasoi.core.contract.bills.BillChargeEditDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.ChargeKindDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.Category
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.UserId
import kotlinx.datetime.LocalDate

/** Review warnings are not mapped: the app derives them with the same `BillRules` as the server. */
internal fun BillDto.toModel() = Bill(
    id = BillId(id),
    householdId = HouseholdId(householdId),
    platform = Platform.valueOf(platform.name),
    invoiceNumber = invoiceNumber,
    orderId = orderId,
    invoiceDate = LocalDate.parse(invoiceDate),
    payerId = UserId(payerId),
    createdBy = UserId(createdBy),
    status = BillStatus.valueOf(status.name),
    confidence = ImportConfidence.valueOf(confidence.name),
    lines = lines.map { it.toModel() },
    charges = charges.map { it.toModel() },
    total = Money(totalPaise),
)

internal fun BillLineDto.toModel() = BillLine(
    id = BillLineId(id),
    rawName = rawName,
    displayName = displayName,
    units = units,
    amount = Money(amountPaise),
    measure = measure?.toModel(),
    mrp = mrpPaise?.let(::Money),
    hsn = hsn,
    upc = upc,
    category = Category.valueOf(category.name),
    isFood = isFood,
)

internal fun BillChargeDto.toModel() =
    BillCharge(ChargeId(id), ChargeKind.valueOf(kind.name), label, Money(amountPaise))

internal fun BillEdit.toDto() = UpdateBillRequestDto(
    lines = lines?.map { BillLineEditDto(it.id?.value, it.displayName, it.units, it.amount.paise, it.isFood) },
    charges = charges?.map {
        BillChargeEditDto(it.id?.value, ChargeKindDto.valueOf(it.kind.name), it.label, it.amount.paise)
    },
    totalPaise = total?.paise,
)
