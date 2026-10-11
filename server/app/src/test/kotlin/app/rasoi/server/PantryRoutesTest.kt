package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.QuantityDto
import app.rasoi.core.contract.bills.QuantityUnitDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.contract.pantry.PantryItemDto
import app.rasoi.core.contract.pantry.PantryStatusDto
import app.rasoi.core.contract.pantry.UpdatePantryItemRequestDto
import app.rasoi.server.support.Invoices
import app.rasoi.server.support.TestScope
import app.rasoi.server.support.authed
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PantryRoutesTest {
    private suspend fun TestScope.household(owner: SessionDto): HouseholdDto = client.post("/households") {
        authed(owner)
        setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
    }.body()

    private suspend fun TestScope.join(owner: SessionDto, home: HouseholdDto, who: SessionDto, role: RoleDto) {
        val invite: InviteDto = client.post("/households/${home.id}/invites") {
            authed(owner)
            setBody(CreateInviteRequestDto(role))
        }.body()
        client.post("/invites/${invite.code}/accept") {
            authed(who)
            setBody("{}")
        }
    }

    private suspend fun TestScope.import(owner: SessionDto, home: HouseholdDto, platform: String): BillDto =
        client.post("/households/${home.id}/invoices") {
            authed(owner)
            setBody(Invoices.upload(Invoices.pdf(platform)))
        }.body()

    private suspend fun TestScope.pantry(who: SessionDto, home: HouseholdDto, query: String = ""): List<PantryItemDto> =
        client.get("/households/${home.id}/pantry$query") { authed(who, null) }.body()

    private suspend fun TestScope.add(who: SessionDto, home: HouseholdDto, body: CreatePantryItemRequestDto) =
        client.post("/households/${home.id}/pantry") {
            authed(who)
            setBody(body)
        }

    private suspend fun TestScope.update(
        who: SessionDto,
        home: HouseholdDto,
        id: String,
        body: UpdatePantryItemRequestDto,
    ): HttpResponse = client.patch("/households/${home.id}/pantry/$id") {
        authed(who)
        setBody(body)
    }

    private fun rice(locationId: String?) = CreatePantryItemRequestDto(
        name = "Basmati rice",
        category = CategoryDto.STAPLES,
        quantity = QuantityDto(5_000_000, QuantityUnitDto.MILLIGRAM),
        locationId = locationId,
    )

    @Test
    fun importedFoodLinesFillThePantryWithPlacesAndExpiryEstimates() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val bill = import(owner, home, "handpickd")
        val items = pantry(owner, home)
        assertEquals(bill.lines.size, items.size)
        assertEquals(bill.lines.map { it.id }.toSet(), items.mapNotNull { it.sourceLineId }.toSet())

        val beans = items.single { it.name.startsWith("Beans") }
        assertEquals(CategoryDto.PRODUCE, beans.category)
        assertEquals(QuantityDto(360_000, QuantityUnitDto.MILLIGRAM), beans.quantity)
        assertEquals("2026-09-28", beans.expiresOn)
        assertEquals("2026-09-23", beans.addedOn)
        assertEquals(2_800, beans.pricePaise)
        val fridge = home.locations.single { it.name == "Fridge" }
        assertEquals(fridge.id, beans.locationId)
        assertEquals(PantryStatusDto.IN_STOCK, beans.status)
    }

    @Test
    fun nonFoodLinesStayOutOfThePantryUntilMarkedAsFood() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val bill = import(owner, home, "blinkit")
        val pack = bill.lines.single()
        assertEquals(CategoryDto.HOUSEHOLD to false, pack.category to pack.isFood)
        assertTrue(pantry(owner, home).isEmpty())

        fun foodEdit(isFood: Boolean) = UpdateBillRequestDto(
            lines = listOf(BillLineEditDto(pack.id, pack.displayName, pack.units, pack.amountPaise, isFood)),
        )
        client.patch("/bills/${bill.id}") {
            authed(owner)
            setBody(foodEdit(true))
        }
        val added = pantry(owner, home).single()
        assertEquals(pack.id, added.sourceLineId)
        assertEquals(QuantityDto(1_000, QuantityUnitDto.COUNT), added.quantity)

        client.patch("/bills/${bill.id}") {
            authed(owner)
            setBody(foodEdit(false))
        }
        assertTrue(pantry(owner, home).isEmpty())
    }

    @Test
    fun itemsAreAddedAndEditedByHandIncludingMovesAndStatus() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val shelf = home.locations.single { it.name == "Shelf" }
        val fridge = home.locations.single { it.name == "Fridge" }

        val created = add(owner, home, rice(shelf.id))
        assertEquals(HttpStatusCode.Created, created.status)
        val item: PantryItemDto = created.body()
        assertEquals("2027-04-09", item.expiresOn)
        assertEquals(shelf.id, item.locationId)

        val moved: PantryItemDto = update(
            owner,
            home,
            item.id,
            UpdatePantryItemRequestDto(
                name = "Brown rice",
                locationId = fridge.id,
                quantity = QuantityDto(2_000_000, QuantityUnitDto.MILLIGRAM),
                expiresOn = "2026-12-01",
            ),
        ).body()
        assertEquals("Brown rice", moved.name)
        assertEquals(fridge.id, moved.locationId)
        assertEquals("2026-12-01", moved.expiresOn)

        val cleared: PantryItemDto = update(owner, home, item.id, UpdatePantryItemRequestDto(clearExpiry = true)).body()
        assertEquals(null, cleared.expiresOn)

        update(owner, home, item.id, UpdatePantryItemRequestDto(status = PantryStatusDto.FINISHED))
        assertTrue(pantry(owner, home).isEmpty())
        assertEquals(listOf(PantryStatusDto.FINISHED), pantry(owner, home, "?status=finished").map { it.status })
        assertEquals(1, pantry(owner, home, "?status=all").size)
        assertEquals(
            "invalid",
            client.get("/households/${home.id}/pantry?status=nope") { authed(owner, null) }
                .body<ErrorDto>().fields.single().code,
        )
    }

    @Test
    fun invalidInputReportsFieldsAndKeepsOtherHouseholdsPlacesOut() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val other = signIn("+919800000099", "Eve")
        val home = household(owner)
        val elsewhere = household(other)

        val blank = add(owner, home, rice(null).copy(name = " "))
        assertEquals(HttpStatusCode.BadRequest, blank.status)
        assertEquals("name" to "required", blank.body<ErrorDto>().fields.single().let { it.field to it.code })
        val zero = add(owner, home, rice(null).copy(quantity = QuantityDto(0, QuantityUnitDto.COUNT)))
        assertEquals("quantity", zero.body<ErrorDto>().fields.single().field)
        val foreign = add(owner, home, rice(elsewhere.locations.first().id))
        assertEquals("locationId" to "unknown", foreign.body<ErrorDto>().fields.single().let { it.field to it.code })
        val badDate = add(owner, home, rice(null).copy(expiresOn = "tomorrow"))
        assertEquals("malformed", badDate.body<ErrorDto>().fields.single().code)
        val item: PantryItemDto = add(owner, home, rice(null)).body()
        val empty = update(owner, home, item.id, UpdatePantryItemRequestDto())
        assertEquals("empty", empty.body<ErrorDto>().fields.single().code)
    }

    @Test
    fun cooksEditThePantryAndOutsidersSeeNothing() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val cook = signIn("+919800000002", "Lakshmi")
        val outsider = signIn("+919800000099", "Eve")
        val home = household(owner)
        val elsewhere = household(outsider)
        join(owner, home, cook, RoleDto.COOK)

        val item: PantryItemDto = add(cook, home, rice(null)).body()
        assertEquals(HttpStatusCode.OK, update(cook, home, item.id, UpdatePantryItemRequestDto(name = "Rice")).status)
        assertEquals(listOf("Rice"), pantry(cook, home).map { it.name })

        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/households/${home.id}/pantry") {
                authed(outsider, null)
            }.status,
        )
        assertEquals(HttpStatusCode.NotFound, add(outsider, home, rice(null)).status)
        assertEquals(
            HttpStatusCode.NotFound,
            update(outsider, home, item.id, UpdatePantryItemRequestDto(name = "x")).status,
        )
        // A member of another household cannot reach this item through their own household either.
        assertEquals(
            HttpStatusCode.NotFound,
            update(outsider, elsewhere, item.id, UpdatePantryItemRequestDto(name = "x")).status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            update(owner, home, "not-a-uuid", UpdatePantryItemRequestDto(name = "x")).status,
        )
        assertNotNull(pantry(owner, home).singleOrNull())
    }
}
