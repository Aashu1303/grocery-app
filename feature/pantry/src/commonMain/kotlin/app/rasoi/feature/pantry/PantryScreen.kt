package app.rasoi.feature.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AppBottomSheet
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.ChoiceChips
import app.rasoi.core.designsystem.component.ChoiceOption
import app.rasoi.core.designsystem.component.ConfirmDialog
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.PantryItemRow
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.domain.pantry.ExpiryRules
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.Role
import app.rasoi.core.model.format
import app.rasoi.feature.pantry.resources.Res
import app.rasoi.feature.pantry.resources.pantry_add
import app.rasoi.feature.pantry.resources.pantry_add_title
import app.rasoi.feature.pantry.resources.pantry_discard_confirm
import app.rasoi.feature.pantry.resources.pantry_discard_message
import app.rasoi.feature.pantry.resources.pantry_discard_title
import app.rasoi.feature.pantry.resources.pantry_empty_filtered_message
import app.rasoi.feature.pantry.resources.pantry_empty_filtered_title
import app.rasoi.feature.pantry.resources.pantry_empty_message
import app.rasoi.feature.pantry.resources.pantry_empty_title
import app.rasoi.feature.pantry.resources.pantry_filter_all
import app.rasoi.feature.pantry.resources.pantry_load_error
import app.rasoi.feature.pantry.resources.pantry_no_place
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The screen's ViewModel, keyed per [key]; the app creates all tab ViewModels up front so tabs open already loaded. */
@Composable
fun pantryViewModel(householdId: HouseholdId, myRole: Role, key: String = householdId.value): PantryViewModel =
    koinViewModel(key = key) { parametersOf(householdId, myRole) }

@Composable
fun PantryRoute(
    householdId: HouseholdId,
    myRole: Role,
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = pantryViewModel(householdId, myRole),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PantryScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

/** In-stock items, soonest expiry first, with storage-place filters and the add, move and finish sheets. */
@Composable
fun PantryScreen(state: PantryUiState, onEvent: (PantryUiEvent) -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        PantryUiState.Loading -> LoadingView(modifier)
        is PantryUiState.Error -> ErrorView(
            message = stringResource(Res.string.pantry_load_error, state.error.text()),
            onRetry = { onEvent(PantryUiEvent.Retry) },
            modifier = modifier,
        )
        is PantryUiState.Content -> {
            PantryContent(state, onEvent, modifier)
            PantrySheets(state, onEvent)
        }
    }
}

@Composable
private fun PantryContent(state: PantryUiState.Content, onEvent: (PantryUiEvent) -> Unit, modifier: Modifier) {
    val noPlace = stringResource(Res.string.pantry_no_place)
    val names = state.locations.associate { it.id to it.name }
    Column(modifier.fillMaxSize()) {
        if (state.items.isNotEmpty()) {
            val all = ChoiceOption<LocationId?>(null, stringResource(Res.string.pantry_filter_all))
            ChoiceChips(
                options = listOf(all) + state.locations.map { ChoiceOption<LocationId?>(it.id, it.name) },
                selected = state.filter,
                onSelect = { onEvent(PantryUiEvent.FilterSelected(it)) },
                modifier = Modifier.padding(horizontal = AppTheme.spacing.screenPadding),
            )
        }
        val items = state.visibleItems
        if (items.isEmpty()) {
            Empty(state, onEvent, Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = AppTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
            ) {
                items(items, key = { it.id.value }) { item ->
                    ItemRow(item, names[item.locationId] ?: noPlace, state) { onEvent(PantryUiEvent.OpenItem(item.id)) }
                }
            }
        }
        if (state.canEdit && items.isNotEmpty()) {
            AppButton(
                text = stringResource(Res.string.pantry_add),
                onClick = { onEvent(PantryUiEvent.OpenAdd) },
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.screenPadding),
            )
        }
    }
}

@Composable
private fun ItemRow(item: PantryItem, location: String, state: PantryUiState.Content, onClick: () -> Unit) {
    PantryItemRow(
        name = item.name,
        quantityText = item.quantity.format(),
        locationText = location,
        freshness = item.expiresOn?.let { ExpiryRules.freshness(it, state.today) } ?: Freshness.FRESH,
        expiryText = item.expiryText(state.today),
        onClick = if (state.canEdit) onClick else null,
    )
}

@Composable
private fun Empty(state: PantryUiState.Content, onEvent: (PantryUiEvent) -> Unit, modifier: Modifier) {
    val filtered = state.items.isNotEmpty()
    EmptyView(
        title = stringResource(if (filtered) Res.string.pantry_empty_filtered_title else Res.string.pantry_empty_title),
        message = stringResource(
            if (filtered) Res.string.pantry_empty_filtered_message else Res.string.pantry_empty_message,
        ),
        actionLabel = if (state.canEdit && !filtered) stringResource(Res.string.pantry_add) else null,
        onAction = if (state.canEdit && !filtered) ({ onEvent(PantryUiEvent.OpenAdd) }) else null,
        modifier = modifier,
    )
}

@Composable
private fun PantrySheets(state: PantryUiState.Content, onEvent: (PantryUiEvent) -> Unit) {
    when (val sheet = state.sheet) {
        null -> Unit
        is PantrySheet.Add -> AppBottomSheet(
            title = stringResource(Res.string.pantry_add_title),
            onDismiss = { onEvent(PantryUiEvent.DismissSheet) },
        ) { AddItemContent(sheet.form, state.locations, onEvent) }
        is PantrySheet.Details -> state.items.firstOrNull { it.id == sheet.itemId }?.let { item ->
            AppBottomSheet(title = item.name, onDismiss = { onEvent(PantryUiEvent.DismissSheet) }) {
                ItemDetailsContent(item, sheet, state, onEvent)
            }
        }
        is PantrySheet.ConfirmDiscard -> state.items.firstOrNull { it.id == sheet.itemId }?.let { item ->
            ConfirmDialog(
                title = stringResource(Res.string.pantry_discard_title),
                message = stringResource(Res.string.pantry_discard_message, item.name),
                confirmLabel = stringResource(Res.string.pantry_discard_confirm),
                onConfirm = { onEvent(PantryUiEvent.Change.ConfirmDiscard) },
                onDismiss = { onEvent(PantryUiEvent.DismissSheet) },
            )
        }
    }
}
