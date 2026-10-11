package app.rasoi.core.data.repository

import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.CreateLocationRequestDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.domain.household.HouseholdDetails
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.household.HouseholdSummary
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Invite
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.core.model.map

class DefaultHouseholdRepository(private val api: RasoiApi) : HouseholdRepository {
    override suspend fun myHouseholds(): AppResult<List<HouseholdSummary>> = api.me().map { me ->
        me.households.map { HouseholdSummary(household(it.id, it.name, it.timeZone), it.role.toModel()) }
    }

    override suspend fun create(name: String, timeZoneId: String): AppResult<HouseholdDetails> =
        api.createHousehold(CreateHouseholdRequestDto(name, timeZoneId)).map { it.toModel() }

    override suspend fun details(id: HouseholdId): AppResult<HouseholdDetails> =
        api.household(id.value).map { it.toModel() }

    override suspend fun createInvite(id: HouseholdId, role: Role): AppResult<Invite> =
        api.createInvite(id.value, CreateInviteRequestDto(role.toDto())).map { it.toModel() }

    override suspend fun acceptInvite(code: String): AppResult<HouseholdDetails> =
        api.acceptInvite(code).map { it.toModel() }

    override suspend fun addLocation(id: HouseholdId, name: String, kind: LocationKind): AppResult<Location> =
        api.addLocation(id.value, CreateLocationRequestDto(name, LocationKindDto.valueOf(kind.name))).map {
            it.toModel(id)
        }
}
