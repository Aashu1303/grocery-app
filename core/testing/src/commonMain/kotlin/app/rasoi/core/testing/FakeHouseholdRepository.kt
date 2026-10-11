package app.rasoi.core.testing

import app.rasoi.core.domain.household.HouseholdDetails
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.household.HouseholdSummary
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Invite
import app.rasoi.core.model.InviteCode
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import kotlin.time.Instant

/** In-memory household repository; set [failure] to make the next calls fail with a typed error. */
class FakeHouseholdRepository(var details: HouseholdDetails? = null, var role: Role = Role.OWNER) :
    HouseholdRepository {
    var failure: AppError? = null
    val created = mutableListOf<Pair<String, String>>()
    val acceptedCodes = mutableListOf<String>()

    override suspend fun myHouseholds(): AppResult<List<HouseholdSummary>> = result {
        listOfNotNull(details?.let { HouseholdSummary(it.household, role) })
    }

    override suspend fun create(name: String, timeZoneId: String): AppResult<HouseholdDetails> = result {
        created += name to timeZoneId
        Builders.details(Builders.household(name = name)).also { details = it }
    }

    override suspend fun details(id: HouseholdId): AppResult<HouseholdDetails> = failure?.let { AppResult.Failure(it) }
        ?: details?.let { AppResult.Success(it) }
        ?: AppResult.Failure(AppError.NotFound("household"))

    override suspend fun createInvite(id: HouseholdId, role: Role): AppResult<Invite> = result {
        Invite(InviteCode("ABCD2345"), id, role, Instant.parse("2026-10-18T10:00:00Z"))
    }

    override suspend fun acceptInvite(code: String): AppResult<HouseholdDetails> = result {
        acceptedCodes += code
        Builders.details().also { details = it }
    }

    override suspend fun addLocation(id: HouseholdId, name: String, kind: LocationKind): AppResult<Location> = result {
        Location(LocationId("l${name.length}"), id, name, kind).also { added ->
            details = details?.let { it.copy(locations = it.locations + added) }
        }
    }

    private inline fun <T> result(block: () -> T): AppResult<T> =
        failure?.let { AppResult.Failure(it) } ?: AppResult.Success(block())
}
