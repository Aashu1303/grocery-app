package app.rasoi.core.domain.household

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Invite
import app.rasoi.core.model.Role
import app.rasoi.core.model.flatMap

/** Checks the role rule locally (ADR 0014) before asking the server, which enforces it again. */
class CreateInviteUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role, inviteeRole: Role): AppResult<Invite> =
        HouseholdRules.canInvite(myRole, inviteeRole).flatMap { repository.createInvite(householdId, inviteeRole) }
}
