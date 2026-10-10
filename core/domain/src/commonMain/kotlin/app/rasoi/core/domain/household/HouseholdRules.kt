package app.rasoi.core.domain.household

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Invite
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Validation and invariants for households, members and invites, shared by app and server. */
object HouseholdRules {
    private const val MAX_HOUSEHOLD_NAME = 60
    private const val MAX_DISPLAY_NAME = 40
    private val INVITE_VALIDITY = 7.days

    val defaultLocations = listOf(
        DefaultLocation("Fridge", LocationKind.FRIDGE),
        DefaultLocation("Freezer", LocationKind.FREEZER),
        DefaultLocation("Shelf", LocationKind.SHELF),
    )

    fun validateName(name: String): AppResult<String> = boundedText("name", name, MAX_HOUSEHOLD_NAME)

    fun validateDisplayName(name: String): AppResult<String> = boundedText("displayName", name, MAX_DISPLAY_NAME)

    fun validateLocationName(name: String): AppResult<String> = boundedText("name", name, MAX_DISPLAY_NAME)

    fun validateTimeZone(id: String): AppResult<TimeZone> = try {
        AppResult.Success(TimeZone.of(id))
    } catch (_: IllegalTimeZoneException) {
        invalid("timeZone", "unknown")
    }

    /** Owners may invite anyone; nobody else may invite. */
    fun canInvite(inviter: Role, invitee: Role): AppResult<Unit> =
        if (RolePolicy.allows(inviter, Permission.INVITE_MEMBERS) && (invitee != Role.OWNER || inviter == Role.OWNER)) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(AppError.Forbidden)
        }

    fun checkAcceptable(invite: Invite, now: Instant): AppResult<Invite> = when {
        invite.acceptedBy != null -> AppResult.Failure(AppError.Conflict("invite_used"))
        now >= invite.expiresAt -> AppResult.Failure(AppError.Conflict("invite_expired"))
        else -> AppResult.Success(invite)
    }

    fun inviteExpiry(now: Instant): Instant = now + INVITE_VALIDITY

    private fun boundedText(field: String, raw: String, max: Int): AppResult<String> {
        val value = raw.trim()
        return when {
            value.isEmpty() -> invalid(field, "blank")
            value.length > max -> invalid(field, "too_long")
            else -> AppResult.Success(value)
        }
    }

    private fun <T> invalid(field: String, code: String): AppResult<T> =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))
}
