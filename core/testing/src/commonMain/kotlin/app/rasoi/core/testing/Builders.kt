package app.rasoi.core.testing

import app.rasoi.core.domain.household.HouseholdDetails
import app.rasoi.core.model.Household
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Member
import app.rasoi.core.model.PhoneNumber
import app.rasoi.core.model.Role
import app.rasoi.core.model.User
import app.rasoi.core.model.UserId
import kotlinx.datetime.TimeZone

/** Test data builders with sensible defaults; override only what a test cares about. */
object Builders {
    fun user(id: String = "u1", name: String = "Meera", phone: String = "+919812345678") =
        User(UserId(id), requireNotNull(PhoneNumber.parse(phone)), name)

    fun household(id: String = "h1", name: String = "Flat 4B") =
        Household(HouseholdId(id), name, TimeZone.of("Asia/Kolkata"))

    fun member(id: String = "u1", name: String = "Meera", role: Role = Role.OWNER) = Member(UserId(id), name, role)

    fun location(id: String = "l1", name: String = "Fridge", kind: LocationKind = LocationKind.FRIDGE) =
        Location(LocationId(id), HouseholdId("h1"), name, kind)

    fun details(
        household: Household = household(),
        members: List<Member> = listOf(member()),
        locations: List<Location> = listOf(location()),
    ) = HouseholdDetails(household, members, locations)
}
