package app.rasoi.server.persistence

import app.rasoi.core.domain.household.HouseholdRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Invite
import app.rasoi.core.model.InviteCode
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant

class InviteStore(private val db: RasoiDatabase) {
    suspend fun create(invite: Invite, createdBy: UserId) = db.query {
        InvitesTable.insert {
            it[code] = invite.code.value
            it[householdId] = invite.householdId.uuid()
            it[role] = invite.role.name
            it[InvitesTable.createdBy] = createdBy.uuid()
            it[expiresAt] = invite.expiresAt
        }
        Unit
    }

    /**
     * Accepts [code] for [userId] atomically: the invite row is locked, checked with [HouseholdRules], marked used,
     * and the membership inserted in the same transaction, so a code can never be used twice.
     */
    suspend fun accept(code: InviteCode, userId: UserId, now: Instant): AppResult<Invite> = db.query {
        val invite = InvitesTable.selectAll().where { InvitesTable.code eq code.value }.forUpdate().singleOrNull()
            ?.toInvite()
            ?: return@query AppResult.Failure(AppError.NotFound("invite"))
        HouseholdRules.checkAcceptable(invite, now).flatMap { acceptable ->
            val alreadyMember = MembershipsTable.selectAll()
                .where {
                    (MembershipsTable.householdId eq acceptable.householdId.uuid()) and
                        (MembershipsTable.userId eq userId.uuid())
                }
                .any()
            if (alreadyMember) {
                AppResult.Failure(AppError.Conflict("already_member"))
            } else {
                InvitesTable.update({ InvitesTable.code eq code.value }) {
                    it[acceptedBy] = userId.uuid()
                    it[acceptedAt] = now
                }
                insertMembership(acceptable.householdId.uuid(), userId.uuid(), acceptable.role, now)
                AppResult.Success(acceptable.copy(acceptedBy = userId))
            }
        }
    }
}

private fun ResultRow.toInvite() = Invite(
    code = InviteCode(this[InvitesTable.code]),
    householdId = this[InvitesTable.householdId].toHouseholdId(),
    role = Role.valueOf(this[InvitesTable.role]),
    expiresAt = this[InvitesTable.expiresAt],
    acceptedBy = this[InvitesTable.acceptedBy]?.toUserId(),
)
