package app.rasoi.core.data

import app.rasoi.core.data.repository.DefaultKitchenRepository
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.HouseholdId
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KitchenRepositoryTest {
    private val household = HouseholdId("h1")

    @Test
    fun readsTheUseFirstListWithItemsAndDaysLeft() = runTest {
        val server = MockServer {
            json(
                """[{"item":{"id":"p1","name":"Milk","category":"DAIRY","quantity":{"amount":1000,"unit":"MILLILITRE"},
                    "locationId":"l1","expiresOn":"2026-10-12","status":"IN_STOCK","addedOn":"2026-10-10"},
                    "daysLeft":1,"freshness":"USE_SOON"},
                   {"item":{"id":"p2","name":"Rice","category":"STAPLES","quantity":{"amount":1000000,"unit":"MILLIGRAM"},
                    "status":"IN_STOCK","addedOn":"2026-10-10"},"freshness":"FRESH"}]""",
            )
        }
        val entries = (DefaultKitchenRepository(server.api).useFirst(household) as AppResult.Success).value
        assertEquals(listOf("Milk", "Rice"), entries.map { it.item.name })
        assertEquals(listOf(1, null), entries.map { it.daysLeft })
        assertEquals(listOf(Freshness.USE_SOON, Freshness.FRESH), entries.map { it.freshness })
        assertEquals("https://api.rasoi.test/households/h1/kitchen/use-first", server.requests.single().url.toString())
    }

    @Test
    fun postsForSuggestionsAndMapsDishes() = runTest {
        val server =
            MockServer { json("""{"dishes":[{"name":"Paneer rice","uses":["paneer","rice"],"summary":"Fry."}]}""") }
        val dishes = (DefaultKitchenRepository(server.api).suggestions(household) as AppResult.Success).value
        assertEquals("Paneer rice", dishes.single().name)
        assertEquals(listOf("paneer", "rice"), dishes.single().uses)
        val request = server.requests.single()
        assertEquals(
            HttpMethod.Post to "https://api.rasoi.test/households/h1/kitchen/suggestions",
            request.method to request.url.toString(),
        )
    }

    @Test
    fun aServerWithoutAModelIsUnavailableNotBroken() = runTest {
        val server =
            MockServer {
                json("""{"code":"suggestions_unavailable","message":"x"}""", HttpStatusCode.ServiceUnavailable)
            }
        assertEquals(
            AppResult.Failure(AppError.Unavailable("suggestions_unavailable")),
            DefaultKitchenRepository(server.api).suggestions(household),
        )
    }
}
