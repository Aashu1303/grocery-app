package app.rasoi.feature.household

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.rasoi.core.domain.auth.AuthState
import app.rasoi.core.domain.auth.CompleteProfileUseCase
import app.rasoi.core.domain.auth.RequestOtpUseCase
import app.rasoi.core.domain.auth.VerifyOtpUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.testing.FakeAuthRepository
import app.rasoi.core.testing.FakePhoneAuthProvider
import app.rasoi.feature.household.signin.SignInStep
import app.rasoi.feature.household.signin.SignInUiEvent
import app.rasoi.feature.household.signin.SignInUiState
import app.rasoi.feature.household.signin.SignInViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SignInViewModelTest : ViewModelTest() {
    private val provider = FakePhoneAuthProvider()
    private val auth = FakeAuthRepository()
    private val savedState = SavedStateHandle()
    private val viewModel = SignInViewModel(
        savedState,
        RequestOtpUseCase(provider),
        VerifyOtpUseCase(provider, auth),
        CompleteProfileUseCase(auth),
    )

    @Test
    fun startsOnThePhoneStep() {
        assertEquals(SignInUiState(), viewModel.state.value)
    }

    @Test
    fun typedPhoneAndNameSurviveProcessDeath() {
        viewModel.onEvent(SignInUiEvent.PhoneChanged("9876543210"))
        viewModel.onEvent(SignInUiEvent.NameChanged("Asha"))
        val restored = SignInViewModel(
            SavedStateHandle(savedState.keys().associateWith { savedState.get<String>(it) }),
            RequestOtpUseCase(provider),
            VerifyOtpUseCase(provider, auth),
            CompleteProfileUseCase(auth),
        )
        assertEquals("9876543210", restored.state.value.phone)
        assertEquals("Asha", restored.state.value.name)
    }

    @Test
    fun firstTimeUserGoesThroughPhoneCodeAndName() = runTest {
        viewModel.state.test {
            awaitItem()
            viewModel.onEvent(SignInUiEvent.PhoneChanged("98123 45678"))
            assertEquals("98123 45678", awaitItem().phone)
            viewModel.onEvent(SignInUiEvent.SubmitPhone)
            assertEquals(SignInStep.CODE, expectMostRecentItem().step)
            assertEquals("+91 •••••• 5678", viewModel.state.value.maskedPhone)

            viewModel.onEvent(SignInUiEvent.CodeChanged("123456"))
            viewModel.onEvent(SignInUiEvent.SubmitCode)
            assertEquals(SignInStep.PROFILE, expectMostRecentItem().step)

            viewModel.onEvent(SignInUiEvent.NameChanged("Meera"))
            viewModel.onEvent(SignInUiEvent.SubmitName)
            assertEquals(false, expectMostRecentItem().isSubmitting)
        }
        assertIs<AuthState.SignedIn>(auth.state.value)
    }

    @Test
    fun invalidPhoneShowsAnInlineErrorAndKeepsInput() {
        viewModel.onEvent(SignInUiEvent.PhoneChanged("123"))
        viewModel.onEvent(SignInUiEvent.SubmitPhone)
        val state = viewModel.state.value
        assertEquals(SignInStep.PHONE, state.step)
        assertEquals(FieldError.INVALID, state.phoneError)
        assertEquals("123", state.phone)

        viewModel.onEvent(SignInUiEvent.PhoneChanged("1234"))
        assertEquals(null, viewModel.state.value.phoneError)
    }

    @Test
    fun wrongCodeIsReportedOnTheCodeField() {
        viewModel.onEvent(SignInUiEvent.PhoneChanged("9812345678"))
        viewModel.onEvent(SignInUiEvent.SubmitPhone)
        viewModel.onEvent(SignInUiEvent.CodeChanged("654321"))
        viewModel.onEvent(SignInUiEvent.SubmitCode)
        assertEquals(FieldError.INCORRECT, viewModel.state.value.codeError)
        assertEquals(SignInStep.CODE, viewModel.state.value.step)
    }

    @Test
    fun changeNumberReturnsToThePhoneStep() {
        viewModel.onEvent(SignInUiEvent.PhoneChanged("9812345678"))
        viewModel.onEvent(SignInUiEvent.SubmitPhone)
        viewModel.onEvent(SignInUiEvent.CodeChanged("12"))
        viewModel.onEvent(SignInUiEvent.ChangeNumber)
        assertEquals(SignInStep.PHONE, viewModel.state.value.step)
        assertEquals("", viewModel.state.value.code)
        assertEquals("9812345678", viewModel.state.value.phone)
    }

    @Test
    fun offlineShowsAScreenError() {
        provider.requestFailure = AppError.Offline
        viewModel.onEvent(SignInUiEvent.PhoneChanged("9812345678"))
        viewModel.onEvent(SignInUiEvent.SubmitPhone)
        assertEquals(ScreenError.OFFLINE, viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.isSubmitting)
    }

    @Test
    fun blankNameIsRejectedInline() {
        viewModel.onEvent(SignInUiEvent.PhoneChanged("9812345678"))
        viewModel.onEvent(SignInUiEvent.SubmitPhone)
        viewModel.onEvent(SignInUiEvent.CodeChanged("123456"))
        viewModel.onEvent(SignInUiEvent.SubmitCode)
        viewModel.onEvent(SignInUiEvent.SubmitName)
        assertEquals(FieldError.REQUIRED, viewModel.state.value.nameError)
    }
}
