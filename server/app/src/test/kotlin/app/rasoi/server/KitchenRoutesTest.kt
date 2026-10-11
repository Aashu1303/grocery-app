package app.rasoi.server

import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.QuantityDto
import app.rasoi.core.contract.bills.QuantityUnitDto
import app.rasoi.core.contract.devices.DevicePlatformDto
import app.rasoi.core.contract.devices.RegisterDeviceRequestDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.contract.kitchen.DishSuggestionsDto
import app.rasoi.core.contract.kitchen.FreshnessDto
import app.rasoi.core.contract.kitchen.UseFirstEntryDto
import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.model.AppResult
import app.rasoi.server.parsing.fallback.ChatCompletionClient
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
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class KitchenRoutesTest {
    private class Flat(val home: HouseholdDto, val meera: SessionDto, val cook: SessionDto)

    private suspend fun TestScope.flat(): Flat {
        val meera = signIn("+919812345678", "Meera")
        val cook = signIn("+919812345681", "Lakshmi")
        val home: HouseholdDto = client.post("/households") {
            authed(meera)
            setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        }.body()
        val invite: InviteDto = client.post("/households/${home.id}/invites") {
            authed(meera)
            setBody(CreateInviteRequestDto(RoleDto.COOK))
        }.body()
        client.post("/invites/${invite.code}/accept") {
            authed(cook)
            setBody("{}")
        }
        return Flat(home, meera, cook)
    }

    private suspend fun TestScope.addItem(
        flat: Flat,
        name: String,
        category: CategoryDto,
        expiresOn: String?,
    ): HttpResponse = client.post("/households/${flat.home.id}/pantry") {
        authed(flat.cook)
        setBody(
            CreatePantryItemRequestDto(
                name = name,
                category = category,
                quantity = QuantityDto(500, QuantityUnitDto.MILLIGRAM),
                expiresOn = expiresOn,
            ),
        )
    }

    private suspend fun TestScope.useFirst(session: SessionDto, flat: Flat): List<UseFirstEntryDto> =
        client.get("/households/${flat.home.id}/kitchen/use-first") { authed(session) }.body()

    @Test
    fun useFirstRanksFoodInStockBySoonestExpiryAndSkipsTheRest() = rasoiTest {
        val flat = flat()
        addItem(flat, "Rice", CategoryDto.STAPLES, null)
        addItem(flat, "Paneer", CategoryDto.DAIRY, "2026-10-14")
        addItem(flat, "Milk", CategoryDto.DAIRY, "2026-10-09")
        addItem(flat, "Dishwash", CategoryDto.HOUSEHOLD, "2026-10-12")
        val ranked = useFirst(flat.cook, flat)
        assertEquals(listOf("Milk", "Paneer", "Rice"), ranked.map { it.item.name })
        // Rice was added without a date, so the server estimated one (design-plan 6.4 item 7).
        assertEquals(listOf(-2, 3, 180), ranked.map { it.daysLeft })
        assertEquals(FreshnessDto.EXPIRED, ranked.first().freshness)
    }

    @Test
    fun suggestionsOnlyUseWhatIsInStockAndNeverSendAnythingAboutPeople() = rasoiTest(
        chat = ChatCompletionClient { _, user ->
            assertTrue("Meera" !in user && "Lakshmi" !in user && "Flat 4B" !in user)
            AppResult.Success(
                """{"dishes":[
                    {"name":"Paneer rice","uses":["paneer","rice"],"summary":"Fry paneer, add rice."},
                    {"name":"Palak paneer","uses":["spinach","paneer"],"summary":"Needs spinach."},
                    {"name":"broken"}]}""",
            )
        },
    ) {
        val flat = flat()
        addItem(flat, "Amul Paneer", CategoryDto.DAIRY, "2026-10-12")
        addItem(flat, "Basmati Rice", CategoryDto.STAPLES, null)
        val response = client.post("/households/${flat.home.id}/kitchen/suggestions") {
            authed(flat.cook)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("Paneer rice"), response.body<DishSuggestionsDto>().dishes.map { it.name })
    }

    @Test
    fun anEmptyKitchenNeedsNoModelAndAMissingOrBrokenModelIsUnavailable() = rasoiTest {
        val flat = flat()
        val path = "/households/${flat.home.id}/kitchen/suggestions"
        suspend fun ask() = client.post(path) {
            authed(flat.meera)
            setBody("{}")
        }
        assertEquals(emptyList(), ask().body<DishSuggestionsDto>().dishes)
        addItem(flat, "Rice", CategoryDto.STAPLES, null)
        assertEquals(HttpStatusCode.ServiceUnavailable, ask().status)
    }

    @Test
    fun aModelThatFailsOrAnswersWithGarbageIsReportedAsUnavailable() = rasoiTest(
        chat = ChatCompletionClient { _, _ -> AppResult.Success("this is not json") },
    ) {
        val flat = flat()
        addItem(flat, "Rice", CategoryDto.STAPLES, null)
        val response = client.post("/households/${flat.home.id}/kitchen/suggestions") {
            authed(flat.meera)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    @Test
    fun nonMembersAreTurnedAwayFromTheKitchen() = rasoiTest {
        val flat = flat()
        val stranger = signIn("+919800000000", "Stranger")
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/households/${flat.home.id}/kitchen/use-first") {
                authed(stranger)
            }.status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.post("/households/${flat.home.id}/kitchen/suggestions") {
                authed(stranger)
                setBody("{}")
            }.status,
        )
    }

    @Test
    fun registeringADeviceIsRepeatableAndValidated() = rasoiTest {
        val flat = flat()
        suspend fun register(token: String) = client.post("/devices") {
            authed(flat.meera)
            setBody(RegisterDeviceRequestDto(token, DevicePlatformDto.ANDROID))
        }
        assertEquals(HttpStatusCode.OK, register("fcm-token-1").status)
        assertEquals(HttpStatusCode.OK, register("fcm-token-1").status)
        assertEquals(HttpStatusCode.BadRequest, register("   ").status)
    }

    @Test
    fun expiryAlertsGoOutOncePerItemAndKindToEveryMemberWithADevice() = rasoiTest {
        val flat = flat()
        listOf(flat.meera to "meera-phone", flat.cook to "cook-phone").forEach { (session, token) ->
            client.post("/devices") {
                authed(session)
                setBody(RegisterDeviceRequestDto(token, DevicePlatformDto.ANDROID))
            }
        }
        addItem(flat, "Milk", CategoryDto.DAIRY, "2026-10-12")
        addItem(flat, "Eggs", CategoryDto.EGGS_MEAT, "2026-10-20")

        assertEquals(1, graph.expiryAlerts.sendDue())
        val soon = notifier.sent.single()
        assertEquals(setOf("meera-phone", "cook-phone"), soon.tokens.toSet())
        assertEquals("Use soon", soon.message.title)
        assertEquals("Milk", soon.message.body)
        assertEquals(0, graph.expiryAlerts.sendDue())

        clock.advance(3.days)
        assertEquals(1, graph.expiryAlerts.sendDue())
        assertEquals(listOf("Use soon", "Expired"), notifier.sent.map { it.message.title })
        assertEquals(0, graph.expiryAlerts.sendDue())
    }

    @Test
    fun deviceTokensTheProviderRejectsAreForgotten() = rasoiTest {
        val flat = flat()
        notifier.rejected += "stale"
        client.post("/devices") {
            authed(flat.meera)
            setBody(RegisterDeviceRequestDto("stale", DevicePlatformDto.IOS))
        }
        addItem(flat, "Milk", CategoryDto.DAIRY, "2026-10-12")
        graph.expiryAlerts.sendDue()
        assertEquals(listOf("stale"), notifier.sent.single().tokens)
        clock.advance(3.days)
        graph.expiryAlerts.sendDue()
        assertEquals(1, notifier.sent.size, "the dropped token leaves nobody to notify")
    }
}
