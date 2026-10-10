package app.rasoi.core.domain.household

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.flatMap

class CreateHouseholdUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(name: String, timeZoneId: String): AppResult<HouseholdDetails> =
        HouseholdRules.validateName(name).flatMap { validName ->
            HouseholdRules.validateTimeZone(timeZoneId).flatMap { zone -> repository.create(validName, zone.id) }
        }
}
