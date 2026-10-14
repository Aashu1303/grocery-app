package app.rasoi.feature.household.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AppBottomSheet
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppPullToRefresh
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.MemberAvatar
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Location
import app.rasoi.core.model.Member
import app.rasoi.core.model.Role
import app.rasoi.feature.household.label
import app.rasoi.feature.household.resources.Res
import app.rasoi.feature.household.resources.household_add_location
import app.rasoi.feature.household.resources.household_invite_title
import app.rasoi.feature.household.resources.household_load_error
import app.rasoi.feature.household.resources.household_locations
import app.rasoi.feature.household.resources.household_members
import app.rasoi.feature.household.text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun HouseholdRoute(
    householdId: HouseholdId,
    myRole: Role,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HouseholdViewModel = koinViewModel(key = householdId.value) { parametersOf(householdId, myRole) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HouseholdScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

@Composable
fun HouseholdScreen(
    state: HouseholdUiState,
    onEvent: (HouseholdUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        val title = (state as? HouseholdUiState.Content)?.details?.household?.name.orEmpty()
        AppTopBar(title = title, onBack = onBack)
        when (state) {
            HouseholdUiState.Loading -> LoadingView()
            is HouseholdUiState.Error -> ErrorView(
                message = stringResource(Res.string.household_load_error, state.error.text()),
                onRetry = { onEvent(HouseholdUiEvent.Retry) },
            )
            is HouseholdUiState.Content -> AppPullToRefresh(
                refreshing = state.isRefreshing,
                onRefresh = { onEvent(HouseholdUiEvent.PullToRefresh) },
            ) { HouseholdContent(state, onEvent) }
        }
    }
    val content = state as? HouseholdUiState.Content
    content?.invite?.let { sheet ->
        AppBottomSheet(title = stringResource(Res.string.household_invite_title), onDismiss = {
            onEvent(HouseholdUiEvent.DismissInvite)
        }) {
            InviteSheetContent(sheet, canInviteOwner = content.myRole == Role.OWNER, onEvent = onEvent)
        }
    }
    content?.addLocation?.let { form ->
        AppBottomSheet(
            title = stringResource(Res.string.household_add_location),
            onDismiss = { onEvent(HouseholdUiEvent.DismissAddLocation) },
        ) { AddLocationContent(form, onEvent) }
    }
}

@Composable
private fun HouseholdContent(state: HouseholdUiState.Content, onEvent: (HouseholdUiEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        item { SectionTitle(stringResource(Res.string.household_members)) }
        items(state.details.members, key = { "member-${it.userId.value}" }) { MemberRow(it) }
        if (state.canInvite) {
            item {
                AppButton(
                    text = stringResource(Res.string.household_invite_title),
                    onClick = { onEvent(HouseholdUiEvent.OpenInvite) },
                    variant = AppButtonVariant.SECONDARY,
                )
            }
        }
        item { SectionTitle(stringResource(Res.string.household_locations)) }
        items(state.details.locations, key = { "location-${it.id.value}" }) { LocationRow(it) }
        if (state.canAddLocation) {
            item {
                AppButton(
                    text = stringResource(Res.string.household_add_location),
                    onClick = { onEvent(HouseholdUiEvent.OpenAddLocation) },
                    variant = AppButtonVariant.SECONDARY,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    AppText(text, style = AppTheme.type.title, heading = true)
}

@Composable
private fun MemberRow(member: Member) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.spacing.minTouchTarget)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(name = member.displayName)
        AppText(member.displayName, modifier = Modifier.weight(1f))
        AppText(member.role.label(), style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun LocationRow(location: Location) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.spacing.minTouchTarget)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(location.name, modifier = Modifier.weight(1f))
        AppText(location.kind.label(), style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant)
    }
}
