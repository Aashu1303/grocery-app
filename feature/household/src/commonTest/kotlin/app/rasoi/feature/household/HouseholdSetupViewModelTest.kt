package app.rasoi.feature.household

import app.cash.turbine.test
import app.rasoi.core.domain.household.CreateHouseholdUseCase
import app.rasoi.core.domain.household.JoinHouseholdUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.testing.FakeHouseholdRepository
import app.rasoi.feature.household.setup.HouseholdSetupEffect
import app.rasoi.feature.household.setup.HouseholdSetupUiEvent
import app.rasoi.feature.household.setup.HouseholdSetupViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HouseholdSetupViewModelTest : ViewModelTest() {
    private val repository = FakeHouseholdRepository()
    private val viewModel = HouseholdSetupViewModel(
        CreateHouseholdUseCase(repository),
        JoinHouseholdUseCase(repository),
        timeZoneId = { "Asia/Kolkata" },
    )

    @Test
    fun creatingEmitsHouseholdReadyInTheDeviceTimeZone() = runTest {
        viewModel.effects.test {
            viewModel.onEvent(HouseholdSetupUiEvent.NameChanged("Flat 4B"))
            viewModel.onEvent(HouseholdSetupUiEvent.Create)
            assertEquals(HouseholdSetupEffect.HouseholdReady(HouseholdId("h1")), awaitItem())
        }
        assertEquals(listOf("Flat 4B" to "Asia/Kolkata"), repository.created)
        assertEquals(false, viewModel.state.value.isCreating)
    }

    @Test
    fun blankNameIsAnInlineError() {
        viewModel.onEvent(HouseholdSetupUiEvent.Create)
        assertEquals(FieldError.REQUIRED, viewModel.state.value.nameError)
    }

    @Test
    fun joiningWithAValidCodeEmitsHouseholdReady() = runTest {
        viewModel.effects.test {
            viewModel.onEvent(HouseholdSetupUiEvent.CodeChanged("abcd 2345"))
            viewModel.onEvent(HouseholdSetupUiEvent.Join)
            assertEquals(HouseholdSetupEffect.HouseholdReady(HouseholdId("h1")), awaitItem())
        }
        assertEquals(listOf("ABCD2345"), repository.acceptedCodes)
    }

    @Test
    fun malformedCodeIsAnInlineError() {
        viewModel.onEvent(HouseholdSetupUiEvent.CodeChanged("abc"))
        viewModel.onEvent(HouseholdSetupUiEvent.Join)
        assertEquals(FieldError.INVALID, viewModel.state.value.codeError)
    }

    @Test
    fun usedOrExpiredInvitesShowAScreenError() {
        repository.failure = AppError.Conflict("invite_used")
        viewModel.onEvent(HouseholdSetupUiEvent.CodeChanged("ABCD2345"))
        viewModel.onEvent(HouseholdSetupUiEvent.Join)
        assertEquals(ScreenError.INVITE_USED, viewModel.state.value.error)

        repository.failure = AppError.Conflict("invite_expired")
        viewModel.onEvent(HouseholdSetupUiEvent.Join)
        assertEquals(ScreenError.INVITE_EXPIRED, viewModel.state.value.error)
        assertEquals("ABCD2345", viewModel.state.value.code)
    }
}
