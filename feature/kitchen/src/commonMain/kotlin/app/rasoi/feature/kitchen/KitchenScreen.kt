package app.rasoi.feature.kitchen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppCard
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.NoticeBanner
import app.rasoi.core.designsystem.component.NoticeTone
import app.rasoi.core.designsystem.component.PantryItemRow
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.UseFirstEntry
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.model.format
import app.rasoi.feature.kitchen.resources.Res
import app.rasoi.feature.kitchen.resources.kitchen_empty_message
import app.rasoi.feature.kitchen.resources.kitchen_empty_title
import app.rasoi.feature.kitchen.resources.kitchen_ideas
import app.rasoi.feature.kitchen.resources.kitchen_ideas_hide
import app.rasoi.feature.kitchen.resources.kitchen_ideas_loading
import app.rasoi.feature.kitchen.resources.kitchen_ideas_none
import app.rasoi.feature.kitchen.resources.kitchen_ideas_title
import app.rasoi.feature.kitchen.resources.kitchen_ideas_unavailable_message
import app.rasoi.feature.kitchen.resources.kitchen_ideas_unavailable_title
import app.rasoi.feature.kitchen.resources.kitchen_ideas_uses
import app.rasoi.feature.kitchen.resources.kitchen_in_stock
import app.rasoi.feature.kitchen.resources.kitchen_load_error
import app.rasoi.feature.kitchen.resources.kitchen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun KitchenRoute(
    householdId: HouseholdId,
    myRole: Role,
    modifier: Modifier = Modifier,
    viewModel: KitchenViewModel = koinViewModel(key = householdId.value) { parametersOf(householdId, myRole) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onEvent(KitchenUiEvent.Refresh) }
    KitchenScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

/** The kitchen's home: food to use first, soonest expiry on top, and dish ideas on request. */
@Composable
fun KitchenScreen(state: KitchenUiState, onEvent: (KitchenUiEvent) -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        KitchenUiState.Loading -> LoadingView(modifier)
        is KitchenUiState.Error -> ErrorView(
            message = stringResource(Res.string.kitchen_load_error, state.error.text()),
            onRetry = { onEvent(KitchenUiEvent.Retry) },
            modifier = modifier,
        )
        is KitchenUiState.Content ->
            if (state.entries.isEmpty()) {
                EmptyView(
                    title = stringResource(Res.string.kitchen_empty_title),
                    message = stringResource(Res.string.kitchen_empty_message),
                    modifier = modifier.padding(AppTheme.spacing.screenPadding),
                )
            } else {
                KitchenContent(state, onEvent, modifier)
            }
    }
}

@Composable
private fun KitchenContent(state: KitchenUiState.Content, onEvent: (KitchenUiEvent) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
    ) {
        item {
            AppText(
                stringResource(Res.string.kitchen_title),
                style = AppTheme.type.title,
                heading = true,
                modifier = Modifier.padding(
                    horizontal = AppTheme.spacing.screenPadding,
                    vertical = AppTheme.spacing.xs,
                ),
            )
        }
        items(state.entries, key = { it.item.id.value }) { entry -> EntryRow(entry) }
        item { Ideas(state, onEvent) }
    }
}

@Composable
private fun EntryRow(entry: UseFirstEntry) {
    PantryItemRow(
        name = entry.item.name,
        quantityText = entry.item.quantity.format(),
        locationText = stringResource(Res.string.kitchen_in_stock),
        freshness = entry.freshness,
        expiryText = expiryText(entry.daysLeft),
    )
}

@Composable
private fun Ideas(state: KitchenUiState.Content, onEvent: (KitchenUiEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        when (val ideas = state.ideas) {
            DishIdeas.Hidden -> AppButton(
                text = stringResource(Res.string.kitchen_ideas),
                onClick = { onEvent(KitchenUiEvent.AskIdeas) },
                modifier = Modifier.fillMaxWidth(),
            )
            DishIdeas.Loading -> AppText(
                stringResource(Res.string.kitchen_ideas_loading),
                color = AppTheme.colors.onSurfaceVariant,
            )
            DishIdeas.Unavailable -> NoticeBanner(
                tone = NoticeTone.INFO,
                title = stringResource(Res.string.kitchen_ideas_unavailable_title),
                message = stringResource(Res.string.kitchen_ideas_unavailable_message),
            )
            is DishIdeas.Failed -> AppText(ideas.error.text(), color = AppTheme.colors.danger)
            is DishIdeas.Ready -> DishList(ideas.dishes)
        }
        if (state.ideas != DishIdeas.Hidden && state.ideas != DishIdeas.Loading) {
            AppButton(
                text = stringResource(Res.string.kitchen_ideas_hide),
                onClick = { onEvent(KitchenUiEvent.HideIdeas) },
                variant = AppButtonVariant.TEXT,
            )
        }
    }
}

@Composable
private fun DishList(dishes: List<DishSuggestion>) {
    AppText(stringResource(Res.string.kitchen_ideas_title), style = AppTheme.type.label, heading = true)
    if (dishes.isEmpty()) {
        AppText(stringResource(Res.string.kitchen_ideas_none), color = AppTheme.colors.onSurfaceVariant)
    }
    dishes.forEach { dish ->
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                AppText(dish.name, style = AppTheme.type.title)
                AppText(
                    stringResource(Res.string.kitchen_ideas_uses, dish.uses.joinToString()),
                    style = AppTheme.type.caption,
                    color = AppTheme.colors.onSurfaceVariant,
                )
                AppText(dish.summary)
            }
        }
    }
}
