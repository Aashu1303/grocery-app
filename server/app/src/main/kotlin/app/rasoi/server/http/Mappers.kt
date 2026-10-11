package app.rasoi.server.http

import app.rasoi.core.contract.auth.MeDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.auth.UserDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.HouseholdSummaryDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.LocationDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.contract.household.MemberDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.model.Household
import app.rasoi.core.model.Invite
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Member
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
