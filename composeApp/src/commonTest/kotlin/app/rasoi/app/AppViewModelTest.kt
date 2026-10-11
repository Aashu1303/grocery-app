package app.rasoi.app

import app.rasoi.core.data.session.InMemorySessionStorage
import app.rasoi.core.data.session.SessionHolder
import app.rasoi.core.model.AppError
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeAuthRepository
import app.rasoi.core.testing.FakeHouseholdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val auth = FakeAuthRepository()
    private val households = FakeHouseholdRepository()
    private val sessions = SessionHolder(InMemorySessionStorage(), Clock.System)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AppViewModel(sessions, auth, households)

    @Test
    fun signedOutUsersSeeSignIn() {
        assertEquals(AppState.SignedOut, viewModel().state.value)
    }

    @Test
    fun signedInWithoutHouseholdNeedsSetupThenBecomesReady() = runTest {
        val viewModel = viewModel()
        auth.signIn("token", "Meera")
        assertIs<AppState.NeedsHousehold>(viewModel.state.value)

        households.details = Builders.details()
        viewModel.refresh()
        val ready = assertIs<AppState.Ready>(viewModel.state.value)
        assertEquals(Role.OWNER, ready.household.role)
    }

    @Test
    fun loadFailureCanBeRetried() = runTest {
        households.failure = AppError.Offline
        val viewModel = viewModel()
        auth.signIn("token", "Meera")
        assertEquals(AppState.LoadFailed, viewModel.state.value)

        households.failure = null
        households.details = Builders.details()
        viewModel.refresh()
        assertIs<AppState.Ready>(viewModel.state.value)
    }

    @Test
    fun signingOutReturnsToSignIn() = runTest {
        val viewModel = viewModel()
        auth.signIn("token", "Meera")
        viewModel.signOut()
        assertEquals(AppState.SignedOut, viewModel.state.value)
    }
}
