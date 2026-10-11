package app.rasoi.server.demo

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.contract.household.RoleDto
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** People and the household the demo creates; later milestones extend the scenario with bills and pantry. */
data class DemoHousehold(
    val owner: SessionDto,
    val flatmate: SessionDto,
    val cook: SessionDto,
    val outsider: SessionDto,
    val household: HouseholdDto,
)

/**
 * The end-to-end demo scenario from design-plan section 8, run against a real server over HTTP. Each step asserts
 * what a user would see. Steps for bills, pantry, kitchen and the ledger are added in the milestones that build them.
 */
class DemoScenario(private val api: DemoApi) {
    suspend fun run(): DemoHousehold {
        assertEquals("ok", api.health().status)
        val demo = signInAndInvite()
        storagePlaces(demo)
        nonMembersSeeNothing(demo)
        return demo
    }

    /** Step 1: two flatmates and a cook sign in; one creates a household and invites the others. */
    private suspend fun signInAndInvite(): DemoHousehold {
        val owner = api.signIn("+919800000001", "Meera Iyer")
        val flatmate = api.signIn("+919800000002", "Aarav Shah")
        val cook = api.signIn("+919800000003", "Lakshmi")
        val outsider = api.signIn("+919800000099", "Eve")
        val created = api.createHousehold(owner, "Demo Flat 4B")

        val memberInvite = api.createdInvite(owner, created.id, RoleDto.MEMBER)
        val cookInvite = api.createdInvite(owner, created.id, RoleDto.COOK)
        api.accept(flatmate, memberInvite.code).expect(HttpStatusCode.OK)
        api.accept(cook, cookInvite.code).expect(HttpStatusCode.OK)
        assertEquals(
            "invite_used",
            api.accept(outsider, memberInvite.code).expect(HttpStatusCode.Conflict).body<ErrorDto>().code,
        )
        api.invite(flatmate, created.id, RoleDto.MEMBER).expect(HttpStatusCode.Forbidden)

        val household: HouseholdDto = api.household(owner, created.id).expect(HttpStatusCode.OK).body()
        assertEquals(
            mapOf("Meera Iyer" to RoleDto.OWNER, "Aarav Shah" to RoleDto.MEMBER, "Lakshmi" to RoleDto.COOK),
            household.members.associate { it.displayName to it.role },
        )
        listOf(owner, flatmate, cook).forEach {
            assertEquals(listOf(created.id), api.me(it).households.map { h -> h.id })
        }
        assertTrue(api.me(outsider).households.isEmpty())
        return DemoHousehold(owner, flatmate, cook, outsider, household)
    }

    /** Storage places used by the pantry: members add them, cooks see them but cannot change them. */
    private suspend fun storagePlaces(demo: DemoHousehold) {
        val id = demo.household.id
        api.addLocation(demo.flatmate, id, "Spice rack", LocationKindDto.SHELF).expect(HttpStatusCode.Created)
        api.addLocation(demo.cook, id, "Balcony", LocationKindDto.OTHER).expect(HttpStatusCode.Forbidden)
        assertEquals(
            listOf("Freezer", "Fridge", "Shelf", "Spice rack"),
            api.locationList(demo.cook, id).map {
                it.name
            },
        )
    }

    /** Step 9: a non-member cannot read or change any of it. */
    private suspend fun nonMembersSeeNothing(demo: DemoHousehold) {
        val id = demo.household.id
        api.household(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.locations(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.addLocation(demo.outsider, id, "Sneaky", LocationKindDto.OTHER).expect(HttpStatusCode.NotFound)
        api.invite(demo.outsider, id, RoleDto.MEMBER).expect(HttpStatusCode.NotFound)
    }
}
