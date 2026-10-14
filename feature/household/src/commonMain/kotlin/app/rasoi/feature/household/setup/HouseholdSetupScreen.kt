package app.rasoi.feature.household.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppCard
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.HouseholdId
import app.rasoi.feature.household.resources.Res
import app.rasoi.feature.household.resources.error_invite_not_found
import app.rasoi.feature.household.resources.error_required
import app.rasoi.feature.household.resources.setup_code_label
import app.rasoi.feature.household.resources.setup_create
import app.rasoi.feature.household.resources.setup_create_title
import app.rasoi.feature.household.resources.setup_join
import app.rasoi.feature.household.resources.setup_join_title
import app.rasoi.feature.household.resources.setup_name_label
import app.rasoi.feature.household.resources.setup_title
import app.rasoi.feature.household.text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HouseholdSetupRoute(
    onHouseholdReady: (HouseholdId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HouseholdSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HouseholdSetupEffect.HouseholdReady -> onHouseholdReady(effect.householdId)
            }
        }
    }
    HouseholdSetupScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun HouseholdSetupScreen(
    state: HouseholdSetupUiState,
    onEvent: (HouseholdSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        AppText(stringResource(Res.string.setup_title), style = AppTheme.type.display, heading = true)
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                AppText(stringResource(Res.string.setup_create_title), style = AppTheme.type.title, heading = true)
                AppTextField(
                    value = state.name,
                    onValueChange = { onEvent(HouseholdSetupUiEvent.NameChanged(it)) },
                    label = stringResource(Res.string.setup_name_label),
                    error = state.nameError?.text(Res.string.error_required),
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppButton(
                    text = stringResource(Res.string.setup_create),
                    onClick = { onEvent(HouseholdSetupUiEvent.Create) },
                    loading = state.isCreating,
                    enabled = !state.isJoining,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                AppText(stringResource(Res.string.setup_join_title), style = AppTheme.type.title, heading = true)
                AppTextField(
                    value = state.code,
                    onValueChange = { onEvent(HouseholdSetupUiEvent.CodeChanged(it)) },
                    label = stringResource(Res.string.setup_code_label),
                    error = state.codeError?.text(Res.string.error_invite_not_found),
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppButton(
                    text = stringResource(Res.string.setup_join),
                    onClick = { onEvent(HouseholdSetupUiEvent.Join) },
                    variant = AppButtonVariant.SECONDARY,
                    loading = state.isJoining,
                    enabled = !state.isCreating,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}
