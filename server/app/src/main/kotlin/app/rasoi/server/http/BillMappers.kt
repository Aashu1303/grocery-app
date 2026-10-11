package app.rasoi.server.http

import app.rasoi.core.contract.bills.AllocationDto
import app.rasoi.core.contract.bills.ApprovalDto
import app.rasoi.core.contract.bills.ApprovalStateDto
import app.rasoi.core.contract.bills.BillChargeDto
import app.rasoi.core.contract.bills.BillChargeEditDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.BillStatusDto
import app.rasoi.core.contract.bills.BillSummaryDto
import app.rasoi.core.contract.bills.BillWarningCodeDto
import app.rasoi.core.contract.bills.BillWarningDto
import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.ChargeDistributionDto
import app.rasoi.core.contract.bills.ChargeKindDto
import app.rasoi.core.contract.bills.CreateBillRequestDto
import app.rasoi.core.contract.bills.ImportConfidenceDto
import app.rasoi.core.contract.bills.LineSplitEditDto
import app.rasoi.core.contract.bills.MemberShareDto
import app.rasoi.core.contract.bills.PlatformDto
import app.rasoi.core.contract.bills.QuantityDto
import app.rasoi.core.contract.bills.QuantityUnitDto
import app.rasoi.core.contract.bills.SplitEntryDto
import app.rasoi.core.contract.bills.SplitModeDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.bills.ChargeEdit
import app.rasoi.core.domain.bills.LineEdit
import app.rasoi.core.domain.bills.LineSplitEdit
import app.rasoi.core.domain.bills.ManualBillDraft
import app.rasoi.core.domain.split.BillShares
import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Approval
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.MemberShare
import app.rasoi.core.model.Money
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import kotlinx.datetime.LocalDate

internal fun Bill.toDto() = BillDto(
    id = id.value,
    householdId = householdId.value,
    platform = PlatformDto.valueOf(platform.name),
    invoiceNumber = invoiceNumber,
    orderId = orderId,
    title = title,
    invoiceDate = invoiceDate.toString(),
    payerId = payerId.value,
    createdBy = createdBy.value,
    status = BillStatusDto.valueOf(status.name),
    confidence = ImportConfidenceDto.valueOf(confidence.name),
    lines = lines.map { it.toDto() },
    charges = charges.map { it.toDto() },
    totalPaise = total.paise,
    warnings = BillRules.warnings(this).map { it.toDto() },
    allocations = allocations.map { it.toDto() },
    approvals = approvals.map { it.toDto() },
    shares = ((BillShares.of(this) as? AppResult.Success)?.value).orEmpty().map { it.toDto() },
    submittedAt = submittedAt,
)

internal fun Bill.toSummaryDto() = BillSummaryDto(
    id = id.value,
    platform = PlatformDto.valueOf(platform.name),
    title = title,
    invoiceDate = invoiceDate.toString(),
    payerId = payerId.value,
    createdBy = createdBy.value,
    status = BillStatusDto.valueOf(status.name),
    totalPaise = total.paise,
    lineCount = lines.size,
)

internal fun BillLine.toDto() = BillLineDto(
    id = id.value,
    rawName = rawName,
    displayName = displayName,
    units = units,
    amountPaise = amount.paise,
    measure = measure?.toDto(),
    mrpPaise = mrp?.paise,
    hsn = hsn,
    upc = upc,
    category = CategoryDto.valueOf(category.name),
    isFood = isFood,
)

internal fun Quantity.toDto() = QuantityDto(amount, QuantityUnitDto.valueOf(unit.name))

internal fun BillCharge.toDto() = BillChargeDto(
    id = id.value,
    kind = ChargeKindDto.valueOf(kind.name),
    label = label,
    amountPaise = amount.paise,
    distribution = ChargeDistributionDto.valueOf(distribution.name),
)

internal fun BillWarning.toDto() = when (this) {
    is BillWarning.TotalMismatch -> BillWarningDto(BillWarningCodeDto.TOTAL_MISMATCH, computed.paise, printed.paise)
    BillWarning.ParsedByFallback -> BillWarningDto(BillWarningCodeDto.PARSED_BY_FALLBACK)
}

internal fun Allocation.toDto() =
    AllocationDto(lineId.value, userId.value, SplitModeDto.valueOf(mode.name), units, amount.paise)

internal fun Approval.toDto() =
    ApprovalDto(userId.value, ApprovalStateDto.valueOf(state.name), comment, decidedAt, automatic)

internal fun MemberShare.toDto() = MemberShareDto(userId.value, lines.paise, charges.paise, total.paise)

internal fun UpdateBillRequestDto.toModel() = BillEdit(
    lines = lines?.map { it.toModel() },
    charges = charges?.map { it.toModel() },
    total = totalPaise?.let(::Money),
    payerId = payerId?.let(::UserId),
    splits = splits?.map { it.toModel() },
)

internal fun CreateBillRequestDto.toModel(caller: UserId, invoiceDate: LocalDate?) = ManualBillDraft(
    title = title,
    invoiceDate = invoiceDate,
    payerId = payerId?.let(::UserId) ?: caller,
    lines = lines.map { it.toModel() },
    charges = charges.map { it.toModel() },
)

private fun BillLineEditDto.toModel() = LineEdit(
    id = id?.takeIf(String::isNotBlank)?.let(::BillLineId),
    displayName = displayName,
    units = units,
    amount = Money(amountPaise),
    isFood = isFood,
)

private fun BillChargeEditDto.toModel() = ChargeEdit(
    id = id?.takeIf(String::isNotBlank)?.let(::ChargeId),
    kind = ChargeKind.valueOf(kind.name),
    label = label,
    amount = Money(amountPaise),
    distribution = distribution?.let { ChargeDistribution.valueOf(it.name) },
)

private fun LineSplitEditDto.toModel() = LineSplitEdit(
    lineId = BillLineId(lineId),
    split = LineSplit(SplitMode.valueOf(mode.name), entries.map { it.toModel() }),
)

private fun SplitEntryDto.toModel() = SplitEntry(UserId(userId), units)
