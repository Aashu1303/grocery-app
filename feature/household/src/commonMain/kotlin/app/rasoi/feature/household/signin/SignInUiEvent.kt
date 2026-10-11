package app.rasoi.feature.household.signin

sealed interface SignInUiEvent {
    data class PhoneChanged(val value: String) : SignInUiEvent

    data object SubmitPhone : SignInUiEvent

    data class CodeChanged(val value: String) : SignInUiEvent

    data object SubmitCode : SignInUiEvent

    data object ChangeNumber : SignInUiEvent

    data class NameChanged(val value: String) : SignInUiEvent

    data object SubmitName : SignInUiEvent
}
