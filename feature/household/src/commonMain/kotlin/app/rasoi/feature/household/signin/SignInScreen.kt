package app.rasoi.feature.household.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppKeyboard
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.feature.household.resources.Res
import app.rasoi.feature.household.resources.error_code_invalid
import app.rasoi.feature.household.resources.error_phone_invalid
import app.rasoi.feature.household.resources.signin_change_number
import app.rasoi.feature.household.resources.signin_code_label
import app.rasoi.feature.household.resources.signin_code_subtitle
import app.rasoi.feature.household.resources.signin_code_title
import app.rasoi.feature.household.resources.signin_continue
import app.rasoi.feature.household.resources.signin_name_label
import app.rasoi.feature.household.resources.signin_phone_label
import app.rasoi.feature.household.resources.signin_profile_title
import app.rasoi.feature.household.resources.signin_send_code
import app.rasoi.feature.household.resources.signin_subtitle
import app.rasoi.feature.household.resources.signin_title
import app.rasoi.feature.household.resources.signin_verify
import app.rasoi.feature.household.text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInRoute(modifier: Modifier = Modifier, viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SignInScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun SignInScreen(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        when (state.step) {
            SignInStep.PHONE -> PhoneStep(state, onEvent)
            SignInStep.CODE -> CodeStep(state, onEvent)
            SignInStep.PROFILE -> ProfileStep(state, onEvent)
        }
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}

@Composable
private fun PhoneStep(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    AppText(stringResource(Res.string.signin_title), style = AppTheme.type.display, heading = true)
    AppText(stringResource(Res.string.signin_subtitle), color = AppTheme.colors.onSurfaceVariant)
    AppTextField(
        value = state.phone,
        onValueChange = { onEvent(SignInUiEvent.PhoneChanged(it)) },
        label = stringResource(Res.string.signin_phone_label),
        error = state.phoneError?.text(Res.string.error_phone_invalid),
        keyboard = AppKeyboard.PHONE,
        modifier = Modifier.fillMaxWidth(),
        onDone = { onEvent(SignInUiEvent.SubmitPhone) },
    )
    AppButton(
        text = stringResource(Res.string.signin_send_code),
        onClick = { onEvent(SignInUiEvent.SubmitPhone) },
        loading = state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CodeStep(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    AppText(stringResource(Res.string.signin_code_title), style = AppTheme.type.title, heading = true)
    AppText(
        stringResource(Res.string.signin_code_subtitle, state.maskedPhone),
        color = AppTheme.colors.onSurfaceVariant,
    )
    AppTextField(
        value = state.code,
        onValueChange = { onEvent(SignInUiEvent.CodeChanged(it)) },
        label = stringResource(Res.string.signin_code_label),
        error = state.codeError?.text(Res.string.error_code_invalid),
        keyboard = AppKeyboard.CODE,
        modifier = Modifier.fillMaxWidth(),
        onDone = { onEvent(SignInUiEvent.SubmitCode) },
    )
    AppButton(
        text = stringResource(Res.string.signin_verify),
        onClick = { onEvent(SignInUiEvent.SubmitCode) },
        loading = state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
    AppButton(
        text = stringResource(Res.string.signin_change_number),
        onClick = { onEvent(SignInUiEvent.ChangeNumber) },
        variant = AppButtonVariant.TEXT,
    )
}

@Composable
private fun ProfileStep(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit) {
    AppText(stringResource(Res.string.signin_profile_title), style = AppTheme.type.title, heading = true)
    AppTextField(
        value = state.name,
        onValueChange = { onEvent(SignInUiEvent.NameChanged(it)) },
        label = stringResource(Res.string.signin_name_label),
        error = state.nameError?.text(Res.string.error_required),
        modifier = Modifier.fillMaxWidth(),
        onDone = { onEvent(SignInUiEvent.SubmitName) },
    )
    AppButton(
        text = stringResource(Res.string.signin_continue),
        onClick = { onEvent(SignInUiEvent.SubmitName) },
        loading = state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}
