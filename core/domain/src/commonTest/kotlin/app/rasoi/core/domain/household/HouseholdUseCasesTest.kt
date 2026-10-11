package app.rasoi.core.domain.household

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeHouseholdRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HouseholdUseCasesTest {
    private val repository = FakeHouseholdRepository(details = Builders.details())
    private val householdId = HouseholdId("h1")

    private fun invalid(field: String, code: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))

    @Test
    fun createValidatesThenCreates() = runTest {
        val create = CreateHouseholdUseCase(repository)
        assertEquals(invalid("name", "blank"), create(" ", "Asia/Kolkata"))
        assertEquals(invalid("timeZone", "unknown"), create("Flat", "Nowhere/City"))
        assertTrue(repository.created.isEmpty())

        assertIs<AppResult.Success<*>>(create("  Flat 4B ", "Asia/Kolkata"))
        assertEquals(listOf("Flat 4B" to "Asia/Kolkata"), repository.created)
    }

    @Test
    fun joinNormalisesTheCode() = runTest {
        val join = JoinHouseholdUseCase(repository)
        assertEquals(invalid("code", "invalid"), join("abc"))
        assertIs<AppResult.Success<*>>(join("abcd-2345"))
        assertEquals(listOf("ABCD2345"), repository.acceptedCodes)
    }

    @Test
    fun onlyOwnersCreateInvites() = runTest {
        val invite = CreateInviteUseCase(repository)
        assertEquals(AppResult.Failure(AppError.Forbidden), invite(householdId, Role.MEMBER, Role.COOK))
        val created = (invite(householdId, Role.OWNER, Role.COOK) as AppResult.Success).value
        assertEquals(Role.COOK, created.role)
    }

    @Test
    fun cooksCannotAddLocations() = runTest {
        val add = AddLocationUseCase(repository)
        assertEquals(AppResult.Failure(AppError.Forbidden), add(householdId, Role.COOK, "Rack", LocationKind.SHELF))
        assertEquals(invalid("name", "blank"), add(householdId, Role.MEMBER, "", LocationKind.SHELF))
        val location = (add(householdId, Role.MEMBER, " Spice rack ", LocationKind.SHELF) as AppResult.Success).value
        assertEquals("Spice rack", location.name)
    }

    @Test
    fun repositoryFailuresPassThrough() = runTest {
        repository.failure = AppError.Offline
        assertEquals(AppResult.Failure(AppError.Offline), JoinHouseholdUseCase(repository)("ABCD2345"))
    }
}
