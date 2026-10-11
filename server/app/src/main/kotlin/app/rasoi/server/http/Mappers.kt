package app.rasoi.server.http

import app.rasoi.core.contract.auth.MeDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.auth.UserDto
import app.rasoi.core.contract.bills.BillChargeDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineDto
import app.rasoi.core.contract.bills.BillStatusDto
import app.rasoi.core.contract.bills.BillWarningCodeDto
import app.rasoi.core.contract.bills.BillWarningDto
import app.rasoi.core.contract.bills.ChargeKindDto
import app.rasoi.core.contract.bills.ImportConfidenceDto
import app.rasoi.core.contract.bills.PlatformDto
import app.rasoi.core.contract.bills.QuantityDto
import app.rasoi.core.contract.bills.QuantityUnitDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.HouseholdSummaryDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.LocationDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.contract.household.MemberDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.bills.ChargeEdit
import app.rasoi.core.domain.bills.LineEdit
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Household
import app.rasoi.core.model.Invite
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Member
import app.rasoi.core.model.Money
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.Role
import app.rasoi.core.model.User
import app.rasoi.server.service.HouseholdDetail
import app.rasoi.server.service.Session

internal fun User.toDto() = UserDto(id.value, displayName, upiVpa)

internal fun Session.toDto() = SessionDto(token, expiresAt, user.toDto())

internal fun Role.toDto() = RoleDto.valueOf(name)

internal fun RoleDto.toModel() = Role.valueOf(name)

internal fun LocationKind.toDto() = LocationKindDto.valueOf(name)

internal fun LocationKindDto.toModel() = LocationKind.valueOf(name)

internal fun Location.toDto() = LocationDto(id.value, name, kind.toDto())

internal fun Member.toDto() = MemberDto(userId.value, displayName, role.toDto())

internal fun Invite.toDto() = InviteDto(code.value, householdId.value, role.toDto(), expiresAt)

internal fun HouseholdDetail.toDto() = HouseholdDto(
    id = household.id.value,
    name = household.name,
    timeZone = household.timeZone.id,
    members = members.map { it.toDto() },
    locations = locations.map { it.toDto() },
)

internal fun Pair<User, List<Pair<Household, Role>>>.toMeDto() = MeDto(
    user = first.toDto(),
    households = second.map { (household, role) ->
        HouseholdSummaryDto(household.id.value, household.name, household.timeZone.id, role.toDto())
    },
)

internal fun Bill.toDto() = BillDto(
    id = id.value,
    householdId = householdId.value,
    platform = PlatformDto.valueOf(platform.name),
    invoiceNumber = invoiceNumber,
    orderId = orderId,
    invoiceDate = invoiceDate.toString(),
    payerId = payerId.value,
    createdBy = createdBy.value,
    status = BillStatusDto.valueOf(status.name),
    confidence = ImportConfidenceDto.valueOf(confidence.name),
    lines = lines.map { it.toDto() },
    charges = charges.map { it.toDto() },
    totalPaise = total.paise,
    warnings = BillRules.warnings(this).map { it.toDto() },
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
)

internal fun Quantity.toDto() = QuantityDto(amount, QuantityUnitDto.valueOf(unit.name))

internal fun BillCharge.toDto() = BillChargeDto(id.value, ChargeKindDto.valueOf(kind.name), label, amount.paise)

internal fun BillWarning.toDto() = when (this) {
    is BillWarning.TotalMismatch -> BillWarningDto(BillWarningCodeDto.TOTAL_MISMATCH, computed.paise, printed.paise)
    BillWarning.ParsedByFallback -> BillWarningDto(BillWarningCodeDto.PARSED_BY_FALLBACK)
}

internal fun UpdateBillRequestDto.toModel() = BillEdit(
    lines = lines?.map { line ->
        LineEdit(
            line.id?.takeIf(String::isNotBlank)?.let(::BillLineId),
            line.displayName,
            line.units,
            Money(line.amountPaise),
        )
    },
    charges = charges?.map { charge ->
        ChargeEdit(
            id = charge.id?.takeIf(String::isNotBlank)?.let(::ChargeId),
            kind = ChargeKind.valueOf(charge.kind.name),
            label = charge.label,
            amount = Money(charge.amountPaise),
        )
    },
    total = totalPaise?.let(::Money),
)
