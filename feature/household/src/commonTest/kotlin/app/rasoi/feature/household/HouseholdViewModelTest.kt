package app.rasoi.feature.household

import app.rasoi.core.domain.household.AddLocationUseCase
import app.rasoi.core.domain.household.CreateInviteUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeHouseholdRepository
import app.rasoi.feature.household.detail.HouseholdUiEvent
import app.rasoi.feature.household.detail.HouseholdUiState
import app.rasoi.feature.household.detail.HouseholdViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class HouseholdViewModelTest : ViewModelTest() {
    private val repository = FakeHouseholdRepository(details = Builders.details())

    private fun viewModel(role: Role = Role.OWNER) = HouseholdViewModel(
        HouseholdId("h1"),
        role,
        repository,
        CreateInviteUseCase(repository),
        AddLocationUseCase(repository),
    )

    private fun HouseholdViewModel.content() = assertIs<HouseholdUiState.Content>(state.value)

    @Test
    fun loadsDetailsWithRolePermissions() {
        val owner = viewModel(Role.OWNER).content()
        assertEquals(true, owner.canInvite)
        assertEquals(true, owner.canAddLocation)

        val cook = viewModel(Role.COOK).content()
        assertEquals(false, cook.canInvite)
        assertEquals(false, cook.canAddLocation)
    }

    @Test
    fun loadFailureShowsErrorAndRetryRecovers() {
        repository.failure = AppError.Offline
        val viewModel = viewModel()
        assertEquals(HouseholdUiState.Error(ScreenError.OFFLINE), viewModel.state.value)

        repository.failure = null
        viewModel.onEvent(HouseholdUiEvent.Retry)
        viewModel.content()
    }

    @Test
    fun ownerCreatesAnInviteCodeForTheChosenRole() {
        val viewModel = viewModel()
        viewModel.onEvent(HouseholdUiEvent.OpenInvite)
        viewModel.onEvent(HouseholdUiEvent.InviteRoleSelected(Role.COOK))
        viewModel.onEvent(HouseholdUiEvent.CreateInvite)
        val invite = requireNotNull(viewModel.content().invite)
        assertEquals(Role.COOK, invite.role)
        assertEquals("ABCD2345", invite.code)

        viewModel.onEvent(HouseholdUiEvent.DismissInvite)
        assertNull(viewModel.content().invite)
    }

    @Test
    fun addedLocationAppearsInTheList() {
        val viewModel = viewModel(Role.MEMBER)
        viewModel.onEvent(HouseholdUiEvent.OpenAddLocation)
        viewModel.onEvent(HouseholdUiEvent.LocationNameChanged("Spice rack"))
        viewModel.onEvent(HouseholdUiEvent.LocationKindSelected(LocationKind.SHELF))
        viewModel.onEvent(HouseholdUiEvent.SaveLocation)
        val content = viewModel.content()
        assertNull(content.addLocation)
        assertEquals(listOf("Fridge", "Spice rack"), content.details.locations.map { it.name })
    }

    @Test
    fun locationErrorsStayInTheForm() {
        val viewModel = viewModel()
        viewModel.onEvent(HouseholdUiEvent.OpenAddLocation)
        viewModel.onEvent(HouseholdUiEvent.SaveLocation)
        assertEquals(FieldError.REQUIRED, viewModel.content().addLocation?.nameError)

        repository.failure = AppError.Conflict("location_exists")
        viewModel.onEvent(HouseholdUiEvent.LocationNameChanged("Fridge"))
        viewModel.onEvent(HouseholdUiEvent.SaveLocation)
        val form = requireNotNull(viewModel.content().addLocation)
        assertEquals(ScreenError.DUPLICATE, form.error)
        assertEquals("Fridge", form.name)
    }

    @Test
    fun pullingToRefreshReloadsAndAFailedPullKeepsWhatIsShown() {
        val viewModel = viewModel()
        val before = viewModel.content()
        viewModel.onEvent(HouseholdUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        repository.failure = AppError.Offline
        viewModel.onEvent(HouseholdUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        assertFalse(viewModel.content().isRefreshing)
    }
}
