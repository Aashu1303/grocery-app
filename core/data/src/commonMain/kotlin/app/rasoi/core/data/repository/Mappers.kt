package app.rasoi.core.data.repository

import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.LocationDto
import app.rasoi.core.contract.household.MemberDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.domain.household.HouseholdDetails
import app.rasoi.core.model.Household
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Invite
import app.rasoi.core.model.InviteCode
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Member
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import kotlinx.datetime.TimeZone

internal fun RoleDto.toModel() = Role.valueOf(name)

internal fun Role.toDto() = RoleDto.valueOf(name)

internal fun household(id: String, name: String, timeZone: String) =
    Household(HouseholdId(id), name, TimeZone.of(timeZone))

internal fun MemberDto.toModel() = Member(UserId(userId), displayName, role.toModel())

internal fun LocationDto.toModel(householdId: HouseholdId) =
    Location(LocationId(id), householdId, name, LocationKind.valueOf(kind.name))

internal fun HouseholdDto.toModel(): HouseholdDetails {
    val household = household(id, name, timeZone)
    return HouseholdDetails(household, members.map { it.toModel() }, locations.map { it.toModel(household.id) })
}

internal fun InviteDto.toModel() = Invite(InviteCode(code), HouseholdId(householdId), role.toModel(), expiresAt)
