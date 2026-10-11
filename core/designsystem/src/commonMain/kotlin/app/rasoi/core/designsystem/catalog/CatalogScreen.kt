package app.rasoi.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_catalog_dark_theme
import app.rasoi.core.designsystem.resources.ds_catalog_light_theme
import app.rasoi.core.designsystem.resources.ds_catalog_title
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Debug-only catalog of every component and state, switchable between light and dark. */
@Composable
fun CatalogScreen(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    DesignSystemCatalog(darkTheme = darkTheme, onToggleTheme = { darkTheme = !darkTheme }, modifier, onBack)
}

@Composable
fun DesignSystemCatalog(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    AppTheme(darkTheme = darkTheme) {
        Column(modifier = modifier.fillMaxSize().background(AppTheme.colors.surface)) {
            AppTopBar(title = stringResource(Res.string.ds_catalog_title), onBack = onBack, actions = {
                AppButton(
                    text = stringResource(
                        if (darkTheme) Res.string.ds_catalog_light_theme else Res.string.ds_catalog_dark_theme,
                    ),
                    onClick = onToggleTheme,
                    variant = AppButtonVariant.TEXT,
                )
            })
            LazyColumn(
                contentPadding = PaddingValues(AppTheme.spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            ) {
                items(catalogEntries, key = { it.id }) { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                        Text(
                            text = "${entry.component} · ${entry.state}",
                            style = AppTheme.type.caption,
                            color = AppTheme.colors.onSurfaceVariant,
                        )
                        entry.content()
                    }
                }
            }
        }
    }
}
