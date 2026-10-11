package app.rasoi.feature.household.signin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.auth.CompleteProfileUseCase
import app.rasoi.core.domain.auth.RequestOtpUseCase
import app.rasoi.core.domain.auth.SignInOutcome
import app.rasoi.core.domain.auth.VerifyOtpUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.feature.household.fieldError
import app.rasoi.feature.household.screenError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Phone, then code, then (first time only) a display name. Signing in updates the auth state, which the app
 * observes to leave this screen, so there is no navigation effect.
 */
@KoinViewModel
class SignInViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val requestOtp: RequestOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase,
    private val completeProfile: CompleteProfileUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        SignInUiState(
            phone = savedStateHandle[KEY_PHONE] ?: "",
            name = savedStateHandle[KEY_NAME] ?: "",
        ),
    )
    val state: StateFlow<SignInUiState> = mutableState.asStateFlow()

    private var verificationId: String? = null
    private var pendingIdToken: String? = null

    fun onEvent(event: SignInUiEvent) {
        when (event) {
            is SignInUiEvent.PhoneChanged -> {
                savedStateHandle[KEY_PHONE] = event.value
                mutableState.update { it.copy(phone = event.value, phoneError = null, error = null) }
            }
            is SignInUiEvent.CodeChanged -> mutableState.update {
                it.copy(code = event.value, codeError = null, error = null)
            }
            is SignInUiEvent.NameChanged -> {
                savedStateHandle[KEY_NAME] = event.value
                mutableState.update { it.copy(name = event.value, nameError = null, error = null) }
            }
            SignInUiEvent.SubmitPhone -> submitPhone()
            SignInUiEvent.SubmitCode -> submitCode()
            SignInUiEvent.SubmitName -> submitName()
            SignInUiEvent.ChangeNumber -> mutableState.update {
                it.copy(step = SignInStep.PHONE, code = "", codeError = null, error = null)
            }
        }
    }

    private fun submitPhone() = submit { state ->
        when (val result = requestOtp(state.phone)) {
            is AppResult.Success -> {
                verificationId = result.value.verificationId
                mutableState.update { it.copy(step = SignInStep.CODE, maskedPhone = result.value.phone.masked()) }
            }
            is AppResult.Failure -> showError(result.error, phoneField = true)
        }
    }

    private fun submitCode() = submit { state ->
        val id = verificationId ?: return@submit mutableState.update { it.copy(step = SignInStep.PHONE) }
        when (val result = verifyOtp(id, state.code)) {
            is AppResult.Success -> when (val outcome = result.value) {
                is SignInOutcome.SignedIn -> Unit
                is SignInOutcome.NeedsProfile -> {
                    pendingIdToken = outcome.idToken
                    mutableState.update { it.copy(step = SignInStep.PROFILE) }
                }
            }
            is AppResult.Failure -> showError(result.error, codeField = true)
        }
    }

    private fun submitName() = submit { state ->
        val token = pendingIdToken ?: return@submit mutableState.update { it.copy(step = SignInStep.PHONE) }
        val result = completeProfile(token, state.name)
        if (result is AppResult.Failure) showError(result.error, nameField = true)
    }

    private fun submit(block: suspend (SignInUiState) -> Unit) {
        val current = mutableState.value
        if (current.isSubmitting) return
        mutableState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            block(current)
            mutableState.update { it.copy(isSubmitting = false) }
        }
    }

    private fun showError(
        error: AppError,
        phoneField: Boolean = false,
        codeField: Boolean = false,
        nameField: Boolean = false,
    ) {
        val phoneError = if (phoneField) error.fieldError("phone") else null
        val codeError = if (codeField) error.fieldError("code") else null
        val nameError = if (nameField) error.fieldError("displayName") else null
        val screenError = if (phoneError == null &&
            codeError == null &&
            nameError == null
        ) {
            error.screenError()
        } else {
            null
        }
        mutableState.update {
            it.copy(phoneError = phoneError, codeError = codeError, nameError = nameError, error = screenError)
        }
    }

    private companion object {
        const val KEY_PHONE = "phone"
        const val KEY_NAME = "name"
    }
}
