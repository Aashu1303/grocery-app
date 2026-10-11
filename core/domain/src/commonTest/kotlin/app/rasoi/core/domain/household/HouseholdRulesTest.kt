package app.rasoi.core.domain.household

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Invite
import app.rasoi.core.model.InviteCode
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class HouseholdRulesTest {
    private val now = Instant.parse("2026-10-11T10:00:00Z")

    @Test
    fun householdNameIsTrimmedAndBounded() {
        assertEquals(AppResult.Success("Flat 4B"), HouseholdRules.validateName("  Flat 4B "))
        assertEquals(violation("name", "blank"), HouseholdRules.validateName("   "))
        assertEquals(violation("name", "too_long"), HouseholdRules.validateName("x".repeat(61)))
    }

    @Test
    fun timeZoneMustBeAnIanaId() {
        val zone = HouseholdRules.validateTimeZone("Asia/Kolkata")
        assertEquals("Asia/Kolkata", (zone as AppResult.Success).value.id)
        assertEquals(violation("timeZone", "unknown"), HouseholdRules.validateTimeZone("Mars/Olympus"))
    }

    @Test
    fun displayNameIsTrimmedAndBounded() {
        assertEquals(AppResult.Success("Meera"), HouseholdRules.validateDisplayName(" Meera "))
        assertEquals(violation("displayName", "blank"), HouseholdRules.validateDisplayName(""))
        assertEquals(violation("displayName", "too_long"), HouseholdRules.validateDisplayName("y".repeat(41)))
    }

    @Test
    fun onlyOwnersMayInviteOwners() {
        assertEquals(AppResult.Success(Unit), HouseholdRules.canInvite(inviter = Role.OWNER, invitee = Role.OWNER))
        assertEquals(AppResult.Success(Unit), HouseholdRules.canInvite(inviter = Role.OWNER, invitee = Role.COOK))
        assertEquals(AppResult.Failure(AppError.Forbidden), HouseholdRules.canInvite(Role.MEMBER, Role.MEMBER))
        assertEquals(AppResult.Failure(AppError.Forbidden), HouseholdRules.canInvite(Role.COOK, Role.COOK))
    }

    @Test
    fun inviteMustBeUnusedAndUnexpired() {
        val invite = Invite(InviteCode("ABCD2345"), HouseholdId("h1"), Role.MEMBER, expiresAt = now + 1.hours)
        assertEquals(AppResult.Success(invite), HouseholdRules.checkAcceptable(invite, now))
        assertEquals(
            AppResult.Failure(AppError.Conflict("invite_expired")),
            HouseholdRules.checkAcceptable(invite, now + 1.hours),
        )
        assertEquals(
            AppResult.Failure(AppError.Conflict("invite_used")),
            HouseholdRules.checkAcceptable(invite.copy(acceptedBy = UserId("u2")), now),
        )
    }

    @Test
    fun invitesExpireAfterSevenDays() {
        assertEquals(Instant.parse("2026-10-18T10:00:00Z"), HouseholdRules.inviteExpiry(now))
    }

    @Test
    fun defaultLocationsCoverFridgeFreezerAndShelf() {
        assertEquals(3, HouseholdRules.defaultLocations.size)
    }

    private fun violation(field: String, code: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))
}
