package app.rasoi.server.service

import app.rasoi.core.domain.household.HouseholdRules
import app.rasoi.core.domain.household.InviteCodeGenerator
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Household
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Invite
import app.rasoi.core.model.InviteCode
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.core.model.User
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.server.persistence.HouseholdStore
import app.rasoi.server.persistence.InviteStore
import app.rasoi.server.persistence.LocationStore
import app.rasoi.server.persistence.UserStore
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Household use cases. Every call first checks the caller's membership and role: non-members get
 * `NotFound` (existence is not revealed), members without the permission get `Forbidden`.
 */
class HouseholdService(
    private val users: UserStore,
    private val households: HouseholdStore,
    private val invites: InviteStore,
    private val locations: LocationStore,
    private val codes: InviteCodeGenerator,
    private val clock: Clock,
) {
    suspend fun me(userId: UserId): AppResult<Pair<User, List<Pair<Household, Role>>>> {
        val user = users.find(userId) ?: return AppResult.Failure(AppError.Unauthenticated)
        return AppResult.Success(user to households.householdsOf(userId))
    }

    suspend fun create(userId: UserId, name: String, timeZone: String): AppResult<HouseholdDetail> =
        HouseholdRules.validateName(name).flatMap { validName ->
            HouseholdRules.validateTimeZone(timeZone).map { zone ->
                households.create(validName, zone, userId, HouseholdRules.defaultLocations, clock.now())
            }
        }.flatMap { detail(userId, it.id) }

    suspend fun detail(userId: UserId, householdId: HouseholdId): AppResult<HouseholdDetail> =
        authorize(userId, householdId, Permission.VIEW_HOUSEHOLD).map {
            val household = requireNotNull(households.find(householdId)) { "Member of a missing household" }
            HouseholdDetail(household, households.members(householdId), locations.list(householdId))
        }

    suspend fun createInvite(userId: UserId, householdId: HouseholdId, role: Role): AppResult<Invite> =
        authorize(userId, householdId, Permission.VIEW_HOUSEHOLD)
            .flatMap { inviterRole -> HouseholdRules.canInvite(inviterRole, role) }
            .map {
                val invite = Invite(codes.next(), householdId, role, HouseholdRules.inviteExpiry(clock.now()))
                invites.create(invite, userId)
                invite
            }

    suspend fun acceptInvite(userId: UserId, rawCode: String): AppResult<HouseholdDetail> {
        val code: InviteCode = InviteCodeGenerator.normalise(rawCode)
            ?: return AppResult.Failure(AppError.NotFound("invite"))
        return invites.accept(code, userId, clock.now()).flatMap { detail(userId, it.householdId) }
    }

    suspend fun locations(userId: UserId, householdId: HouseholdId): AppResult<List<Location>> =
        authorize(userId, householdId, Permission.VIEW_PANTRY).map { locations.list(householdId) }

    suspend fun addLocation(
        userId: UserId,
        householdId: HouseholdId,
        name: String,
        kind: LocationKind,
    ): AppResult<Location> = authorize(userId, householdId, Permission.MANAGE_LOCATIONS)
        .flatMap { HouseholdRules.validateLocationName(name) }
        .flatMap { validName -> locations.create(householdId, validName, kind) }

    suspend fun timeZone(householdId: HouseholdId): TimeZone =
        requireNotNull(households.find(householdId)) { "Member of a missing household" }.timeZone

    /** Members who can owe a share of a bill: everyone whose role may approve bills, so never a cook. */
    suspend fun billParticipants(householdId: HouseholdId): Set<UserId> =
        households.members(householdId).filter { RolePolicy.allows(it.role, Permission.APPROVE_BILLS) }
            .map { it.userId }.toSet()

    /** Returns the caller's role when they are a member holding [permission]. */
    suspend fun authorize(userId: UserId, householdId: HouseholdId, permission: Permission): AppResult<Role> {
        val role = households.roleOf(householdId, userId) ?: return AppResult.Failure(AppError.NotFound("household"))
        if (!RolePolicy.allows(role, permission)) return AppResult.Failure(AppError.Forbidden)
        return AppResult.Success(role)
    }
}
