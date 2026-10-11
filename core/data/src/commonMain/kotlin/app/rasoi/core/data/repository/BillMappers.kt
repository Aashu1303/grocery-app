package app.rasoi.core.data.repository

import app.rasoi.core.contract.bills.AllocationDto
import app.rasoi.core.contract.bills.ApprovalDto
import app.rasoi.core.contract.bills.BillChargeDto
import app.rasoi.core.contract.bills.BillChargeEditDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.BillSummaryDto
import app.rasoi.core.contract.bills.ChargeDistributionDto
import app.rasoi.core.contract.bills.ChargeKindDto
import app.rasoi.core.contract.bills.CreateBillRequestDto
import app.rasoi.core.contract.bills.LineSplitEditDto
import app.rasoi.core.contract.bills.SplitEntryDto
import app.rasoi.core.contract.bills.SplitModeDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.ChargeEdit
import app.rasoi.core.domain.bills.LineEdit
import app.rasoi.core.domain.bills.LineSplitEdit
import app.rasoi.core.domain.bills.ManualBillDraft
import app.rasoi.core.model.Allocation
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.Category
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import kotlinx.datetime.LocalDate

/**
 * Review warnings and member shares are not mapped: the app derives them from the bill with the same `BillRules` and
 * `BillShares` as the server.
 */
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
    allocations = allocations.map { it.toModel() },
    approvals = approvals.map { it.toModel() },
    submittedAt = submittedAt,
    title = title,
)

internal fun BillSummaryDto.toModel() = BillSummary(
    id = BillId(id),
    platform = Platform.valueOf(platform.name),
    title = title,
    invoiceDate = LocalDate.parse(invoiceDate),
    payerId = UserId(payerId),
    createdBy = UserId(createdBy),
    status = BillStatus.valueOf(status.name),
    total = Money(totalPaise),
    lineCount = lineCount,
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

internal fun BillChargeDto.toModel() = BillCharge(
    id = ChargeId(id),
    kind = ChargeKind.valueOf(kind.name),
    label = label,
    amount = Money(amountPaise),
    distribution = ChargeDistribution.valueOf(distribution.name),
)

private fun AllocationDto.toModel() =
    Allocation(BillLineId(lineId), UserId(userId), SplitMode.valueOf(mode.name), units, Money(amountPaise))

private fun ApprovalDto.toModel() =
    Approval(UserId(userId), ApprovalState.valueOf(state.name), comment, decidedAt, automatic)

internal fun BillEdit.toDto() = UpdateBillRequestDto(
    lines = lines?.map { it.toDto() },
    charges = charges?.map { it.toDto() },
    totalPaise = total?.paise,
    payerId = payerId?.value,
    splits = splits?.map { it.toDto() },
)

internal fun ManualBillDraft.toDto() = CreateBillRequestDto(
    title = title,
    invoiceDate = invoiceDate?.toString(),
    lines = lines.map { it.toDto() },
    charges = charges.map { it.toDto() },
    payerId = payerId.value,
)

private fun LineEdit.toDto() = BillLineEditDto(id?.value, displayName, units, amount.paise, isFood)

private fun ChargeEdit.toDto() = BillChargeEditDto(
    id = id?.value,
    kind = ChargeKindDto.valueOf(kind.name),
    label = label,
    amountPaise = amount.paise,
    distribution = distribution?.let { ChargeDistributionDto.valueOf(it.name) },
)

private fun LineSplitEdit.toDto() = LineSplitEditDto(
    lineId = lineId.value,
    mode = SplitModeDto.valueOf(split.mode.name),
    entries = split.entries.map { SplitEntryDto(it.userId.value, it.units) },
)
