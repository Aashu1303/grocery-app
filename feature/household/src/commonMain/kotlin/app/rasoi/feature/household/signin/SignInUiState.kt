package app.rasoi.feature.household.signin

import app.rasoi.feature.household.FieldError
import app.rasoi.feature.household.ScreenError

enum class SignInStep { PHONE, CODE, PROFILE }

data class SignInUiState(
    val step: SignInStep = SignInStep.PHONE,
    val phone: String = "",
    val maskedPhone: String = "",
    val code: String = "",
    val name: String = "",
    val phoneError: FieldError? = null,
    val codeError: FieldError? = null,
    val nameError: FieldError? = null,
    val error: ScreenError? = null,
    val isSubmitting: Boolean = false,
)
