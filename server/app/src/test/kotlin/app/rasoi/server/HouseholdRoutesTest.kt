package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.MeDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.CreateLocationRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.LocationDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.server.support.TestScope
import app.rasoi.server.support.authed
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class HouseholdRoutesTest {
    private suspend fun TestScope.createHousehold(owner: SessionDto, name: String = "Flat 4B"): HouseholdDto =
        client.post("/households") {
            authed(owner)
            setBody(CreateHouseholdRequestDto(name, "Asia/Kolkata"))
        }.body()

    private suspend fun TestScope.invite(by: SessionDto, householdId: String, role: RoleDto): HttpResponse =
        client.post("/households/$householdId/invites") {
            authed(by)
            setBody(CreateInviteRequestDto(role))
        }

    private suspend fun TestScope.accept(by: SessionDto, code: String): HttpResponse =
        client.post("/invites/$code/accept") {
            authed(by)
            setBody("{}")
        }

    @Test
    fun creatingAHouseholdMakesTheCallerOwnerWithDefaultLocations() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val household = createHousehold(owner)
        assertEquals("Flat 4B", household.name)
        assertEquals(listOf(RoleDto.OWNER), household.members.map { it.role })
        assertEquals(setOf("Fridge", "Freezer", "Shelf"), household.locations.map { it.name }.toSet())

        val me: MeDto = client.get("/me") { authed(owner, null) }.body()
        assertEquals(listOf(household.id), me.households.map { it.id })
    }

    @Test
    fun invalidHouseholdInputReportsFields() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val response = client.post("/households") {
            authed(owner)
            setBody(CreateHouseholdRequestDto(" ", "Mars/Olympus"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("name", response.body<ErrorDto>().fields.single().field)
    }

    @Test
    fun inviteFlowAddsMembersWithTheirRoles() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val flatmate = signIn("+919800000001", "Aarav")
        val cook = signIn("+919800000002", "Lakshmi")
        val household = createHousehold(owner)

        val memberInvite: InviteDto = invite(owner, household.id, RoleDto.MEMBER).body()
        val cookInvite: InviteDto = invite(owner, household.id, RoleDto.COOK).body()
        assertEquals(HttpStatusCode.OK, accept(flatmate, memberInvite.code).status)
        val joined: HouseholdDto = accept(cook, cookInvite.code.lowercase()).body()

        assertEquals(
            mapOf("Meera" to RoleDto.OWNER, "Aarav" to RoleDto.MEMBER, "Lakshmi" to RoleDto.COOK),
            joined.members.associate { it.displayName to it.role },
        )
    }

    @Test
    fun invitesAreSingleUseAndExpire() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val first = signIn("+919800000001", "Aarav")
        val second = signIn("+919800000002", "Ravi")
        val household = createHousehold(owner)
        val used: InviteDto = invite(owner, household.id, RoleDto.MEMBER).body()
        accept(first, used.code)

        val reused = accept(second, used.code)
        assertEquals(HttpStatusCode.Conflict, reused.status)
        assertEquals("invite_used", reused.body<ErrorDto>().code)

        val expiring: InviteDto = invite(owner, household.id, RoleDto.MEMBER).body()
        clock.advance(8.days)
        val ownerAgain = signIn("+919812345678", "Meera")
        val expired = accept(second, expiring.code)
        assertEquals(HttpStatusCode.Conflict, expired.status)
        assertEquals("invite_expired", expired.body<ErrorDto>().code)
        assertEquals(HttpStatusCode.NotFound, accept(ownerAgain, "ZZZZ2222").status)
    }

    @Test
    fun onlyOwnersCanInvite() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val member = signIn("+919800000001", "Aarav")
        val household = createHousehold(owner)
        accept(member, invite(owner, household.id, RoleDto.MEMBER).body<InviteDto>().code)

        assertEquals(HttpStatusCode.Forbidden, invite(member, household.id, RoleDto.MEMBER).status)
    }

    @Test
    fun nonMembersCannotSeeOrChangeAHousehold() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val outsider = signIn("+919800000009", "Eve")
        val household = createHousehold(owner)

        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/households/${household.id}") {
                authed(outsider, null)
            }.status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/households/${household.id}/locations") { authed(outsider, null) }.status,
        )
        assertEquals(HttpStatusCode.NotFound, invite(outsider, household.id, RoleDto.MEMBER).status)
        val addLocation = client.post("/households/${household.id}/locations") {
            authed(outsider)
            setBody(CreateLocationRequestDto("Balcony", LocationKindDto.OTHER))
        }
        assertEquals(HttpStatusCode.NotFound, addLocation.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/households/not-a-uuid") { authed(outsider, null) }.status)
    }

    @Test
    fun locationsCanBeAddedByMembersButNotCooks() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val cook = signIn("+919800000002", "Lakshmi")
        val household = createHousehold(owner)
        accept(cook, invite(owner, household.id, RoleDto.COOK).body<InviteDto>().code)

        val added = client.post("/households/${household.id}/locations") {
            authed(owner)
            setBody(CreateLocationRequestDto("Spice rack", LocationKindDto.SHELF))
        }
        assertEquals(HttpStatusCode.Created, added.status)
        val duplicate = client.post("/households/${household.id}/locations") {
            authed(owner)
            setBody(CreateLocationRequestDto("spice RACK", LocationKindDto.SHELF))
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status)
        val byCook = client.post("/households/${household.id}/locations") {
            authed(cook)
            setBody(CreateLocationRequestDto("Pantry", LocationKindDto.SHELF))
        }
        assertEquals(HttpStatusCode.Forbidden, byCook.status)

        val visibleToCook: List<LocationDto> = client.get("/households/${household.id}/locations") {
            authed(cook, null)
        }.body()
        assertEquals(listOf("Freezer", "Fridge", "Shelf", "Spice rack"), visibleToCook.map { it.name })
    }
}
