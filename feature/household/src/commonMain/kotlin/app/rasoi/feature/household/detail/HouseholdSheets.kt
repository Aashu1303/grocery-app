package app.rasoi.feature.household.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.component.ChoiceChips
import app.rasoi.core.designsystem.component.ChoiceOption
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.feature.household.label
import app.rasoi.feature.household.resources.Res
import app.rasoi.feature.household.resources.error_required
import app.rasoi.feature.household.resources.household_invite_create
import app.rasoi.feature.household.resources.household_invite_done
import app.rasoi.feature.household.resources.household_invite_ready
import app.rasoi.feature.household.resources.household_location_name
import app.rasoi.feature.household.resources.household_save_location
import app.rasoi.feature.household.text
import org.jetbrains.compose.resources.stringResource

/** Choose a role and create a one-time code, then show the code to share. */
@Composable
fun InviteSheetContent(
    state: InviteSheetState,
    canInviteOwner: Boolean,
    onEvent: (HouseholdUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        val code = state.code
        if (code == null) {
            val roles = if (canInviteOwner) Role.entries else Role.entries - Role.OWNER
            ChoiceChips(
                options = roles.map { ChoiceOption(it, it.label()) },
                selected = state.role,
                onSelect = { onEvent(HouseholdUiEvent.InviteRoleSelected(it)) },
            )
            AppButton(
                text = stringResource(Res.string.household_invite_create),
                onClick = { onEvent(HouseholdUiEvent.CreateInvite) },
                loading = state.isCreating,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AppText(code, style = AppTheme.type.display)
            AppText(stringResource(Res.string.household_invite_ready), color = AppTheme.colors.onSurfaceVariant)
            AppButton(
                text = stringResource(Res.string.household_invite_done),
                onClick = { onEvent(HouseholdUiEvent.DismissInvite) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}

/** Name and kind of a new storage place. */
@Composable
fun AddLocationContent(state: AddLocationState, onEvent: (HouseholdUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTextField(
            value = state.name,
            onValueChange = { onEvent(HouseholdUiEvent.LocationNameChanged(it)) },
            label = stringResource(Res.string.household_location_name),
            error = state.nameError?.text(Res.string.error_required),
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceChips(
            options = LocationKind.entries.map { ChoiceOption(it, it.label()) },
            selected = state.kind,
            onSelect = { onEvent(HouseholdUiEvent.LocationKindSelected(it)) },
        )
        AppButton(
            text = stringResource(Res.string.household_save_location),
            onClick = { onEvent(HouseholdUiEvent.SaveLocation) },
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}
