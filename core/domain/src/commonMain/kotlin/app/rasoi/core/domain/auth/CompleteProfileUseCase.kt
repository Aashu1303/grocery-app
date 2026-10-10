package app.rasoi.core.domain.auth

import app.rasoi.core.domain.household.HouseholdRules
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.User
import app.rasoi.core.model.flatMap

class CompleteProfileUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(idToken: String, displayName: String): AppResult<User> =
        HouseholdRules.validateDisplayName(displayName).flatMap { name -> auth.signIn(idToken, name) }
}
