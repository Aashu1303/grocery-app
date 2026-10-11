package app.rasoi.core.data

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.contract.pantry.UpdatePantryItemRequestDto
import app.rasoi.core.data.repository.DefaultPantryRepository
import app.rasoi.core.domain.pantry.NewPantryItem
import app.rasoi.core.domain.pantry.PantryEdit
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Category
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.Money
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PantryRepositoryTest {
    private val household = HouseholdId("h1")
    private val itemJson = """
        {"id":"p1","name":"Beans Lobia","category":"PRODUCE","quantity":{"amount":360000,"unit":"MILLIGRAM"},
         "locationId":"l2","expiresOn":"2026-09-28","status":"IN_STOCK","addedOn":"2026-09-23",
         "pricePaise":2800,"sourceLineId":"line1"}
    """.trimIndent()

    private val base = "https://api.rasoi.test/households/h1/pantry"

    private fun body(server: MockServer) = (server.requests.last().body as TextContent).text

    @Test
    fun listsItemsForAStatusAndMapsThem() = runTest {
        val server = MockServer { json("[$itemJson]") }
        val result = DefaultPantryRepository(server.api).items(household, PantryStatus.IN_STOCK)
        val item = assertIs<AppResult.Success<List<PantryItem>>>(result).value.single()
        assertEquals(
            "$base?status=IN_STOCK",
            server.requests.single().url.toString(),
        )
        assertEquals(Category.PRODUCE, item.category)
        assertEquals(Quantity.grams(360), item.quantity)
        assertEquals(LocationId("l2"), item.locationId)
        assertEquals(LocalDate(2026, 9, 28), item.expiresOn)
        assertEquals(Money(2_800), item.price)
        assertEquals(household, item.householdId)
    }

    @Test
    fun anyStatusIsRequestedAsAll() = runTest {
        val server = MockServer { json("[]") }
        DefaultPantryRepository(server.api).items(household, null)
        assertEquals("all", server.requests.single().url.parameters["status"]?.lowercase())
    }

    @Test
    fun addPostsTheItemWithAnIdempotencyKey() = runTest {
        val server = MockServer { json(itemJson, HttpStatusCode.Created) }
        val draft =
            NewPantryItem("Beans", Category.PRODUCE, Quantity.grams(360), LocationId("l2"), LocalDate(2026, 9, 28))
        assertIs<AppResult.Success<PantryItem>>(DefaultPantryRepository(server.api).add(household, draft))
        val request = server.requests.single()
        assertEquals(
            HttpMethod.Post to base,
            request.method to request.url.toString(),
        )
        assertEquals("key-1", request.headers[ApiHeaders.IDEMPOTENCY_KEY])
        val sent = ContractJson.decodeFromString(CreatePantryItemRequestDto.serializer(), body(server))
        assertEquals("2026-09-28", sent.expiresOn)
        assertEquals(360_000L, sent.quantity.amount)
    }

    @Test
    fun updatePatchesOnlyTheChangedFields() = runTest {
        val server = MockServer { json(itemJson) }
        val edit = PantryEdit(locationId = LocationId("l9"), clearExpiry = true, status = PantryStatus.FINISHED)
        assertIs<AppResult.Success<PantryItem>>(
            DefaultPantryRepository(server.api).update(household, PantryItemId("p1"), edit),
        )
        val request = server.requests.single()
        assertEquals(
            HttpMethod.Patch to "$base/p1",
            request.method to request.url.toString(),
        )
        val sent = ContractJson.decodeFromString(UpdatePantryItemRequestDto.serializer(), body(server))
        assertEquals(null, sent.name)
        assertEquals("l9", sent.locationId)
        assertEquals(true, sent.clearExpiry)
    }

    @Test
    fun failuresAreTyped() = runTest {
        val server =
            MockServer { json("""{"code":"not_found","message":"No such pantry item"}""", HttpStatusCode.NotFound) }
        val result = DefaultPantryRepository(server.api).update(household, PantryItemId("p1"), PantryEdit(name = "x"))
        assertEquals(AppResult.Failure(AppError.NotFound("pantry item")), result)
    }
}
