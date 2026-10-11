package app.rasoi.core.data

import app.cash.turbine.test
import app.rasoi.core.data.repository.DefaultAuthRepository
import app.rasoi.core.data.repository.DefaultHouseholdRepository
import app.rasoi.core.data.session.InMemorySessionStorage
import app.rasoi.core.data.session.SessionHolder
import app.rasoi.core.data.session.StoredSession
import app.rasoi.core.domain.auth.AuthState
import app.rasoi.core.model.Account
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class RepositoriesTest {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-11T00:00:00Z")
    }

    @Test
    fun signInStoresTheSessionAndPublishesSignedIn() = runTest {
        val server = MockServer {
            json("""{"token":"t1","expiresAt":"2026-11-10T00:00:00Z","user":{"id":"u1","displayName":"Meera"}}""")
        }
        val storage = InMemorySessionStorage()
        val holder = SessionHolder(storage, clock)
        val auth = DefaultAuthRepository(server.api, holder, backgroundScope)

        auth.state.test {
            assertEquals(AuthState.SignedOut, awaitItem())
            assertEquals(AppResult.Success(Account(UserId("u1"), "Meera")), auth.signIn("id-token", "Meera"))
            assertEquals(AuthState.SignedIn(Account(UserId("u1"), "Meera")), awaitItem())
            assertEquals("t1", storage.read()?.token)
            auth.signOut()
            assertEquals(AuthState.SignedOut, awaitItem())
        }
        assertEquals(null, storage.read())
    }

    @Test
    fun restoreDropsExpiredSessions() = runTest {
        val expired = StoredSession("t", Instant.parse("2026-10-10T00:00:00Z"), "u1", "Meera")
        val storage = InMemorySessionStorage(expired)
        assertEquals(null, SessionHolder(storage, clock).restore())
        assertEquals(null, storage.read())

        val valid = expired.copy(expiresAt = Instant.parse("2026-12-01T00:00:00Z"))
        val holder = SessionHolder(InMemorySessionStorage(valid), clock)
        assertEquals(valid, holder.restore())
        assertEquals("t", holder.token())
    }

    @Test
    fun householdRepositoryMapsDtos() = runTest {
        val server = MockServer { request ->
            when (request.url.encodedPath) {
                "/me" -> json(
                    """{"user":{"id":"u1","displayName":"Meera"},
                    "households":[{"id":"h1","name":"Flat 4B","timeZone":"Asia/Kolkata","role":"OWNER"}]}""",
                )
                "/households/h1/invites" -> json(
                    """{"code":"ABCD2345","householdId":"h1","role":"COOK","expiresAt":"2026-10-18T10:00:00Z"}""",
                    HttpStatusCode.Created,
                )
                else -> json(
                    """{"id":"l9","name":"Spice rack","kind":"SHELF"}""",
                    HttpStatusCode.Created,
                )
            }
        }
        val repository = DefaultHouseholdRepository(server.api)

        val summaries = (repository.myHouseholds() as AppResult.Success).value
        assertEquals("Asia/Kolkata", summaries.single().household.timeZone.id)
        assertEquals(Role.OWNER, summaries.single().role)

        val invite = (repository.createInvite(HouseholdId("h1"), Role.COOK) as AppResult.Success).value
        assertEquals("ABCD2345", invite.code.value)
        assertEquals("""{"role":"COOK"}""", (server.requests.last().body as TextContent).text)

        val location = (
            repository.addLocation(
                HouseholdId("h1"),
                "Spice rack",
                LocationKind.SHELF,
            ) as AppResult.Success
            ).value
        assertEquals(LocationKind.SHELF, location.kind)
        assertEquals(HouseholdId("h1"), location.householdId)
    }
}
