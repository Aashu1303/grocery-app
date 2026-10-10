package app.rasoi.core.domain.household

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Invite
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role

interface HouseholdRepository {
    suspend fun myHouseholds(): AppResult<List<HouseholdSummary>>

    suspend fun create(name: String, timeZoneId: String): AppResult<HouseholdDetails>

    suspend fun details(id: HouseholdId): AppResult<HouseholdDetails>

    suspend fun createInvite(id: HouseholdId, role: Role): AppResult<Invite>

    suspend fun acceptInvite(code: String): AppResult<HouseholdDetails>

    suspend fun addLocation(id: HouseholdId, name: String, kind: LocationKind): AppResult<Location>
}
