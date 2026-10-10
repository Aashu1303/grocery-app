package app.rasoi.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.rasoi.app.resources.Res
import app.rasoi.app.resources.app_name
import app.rasoi.app.resources.open_catalog
import app.rasoi.core.designsystem.catalog.CatalogScreen
import app.rasoi.core.designsystem.component.AppBottomBar
import app.rasoi.core.designsystem.component.AppBottomBarItem
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Root composable: theme, top bar, bottom bar and the navigation graph. */
@Composable
fun RasoiApp(showCatalog: Boolean, navController: NavHostController = rememberNavController()) {
    AppTheme {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        val selected = TopLevelDestination.entries.firstOrNull { tab ->
            destination?.hasRoute(tab.route::class) == true
        }
        Column(
            Modifier
                .fillMaxSize()
                .background(AppTheme.colors.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (selected != null) {
                AppTopBar(title = stringResource(Res.string.app_name), actions = {
                    if (showCatalog) {
                        AppButton(
                            text = stringResource(Res.string.open_catalog),
                            onClick = { navController.navigate(Route.Catalog) },
                            variant = AppButtonVariant.TEXT,
                        )
                    }
                })
            }
            NavHost(navController, startDestination = Route.Kitchen, modifier = Modifier.weight(1f)) {
                TopLevelDestination.entries.forEach { tab -> placeholder(tab) }
                composable<Route.Catalog> { CatalogScreen(onBack = { navController.popBackStack() }) }
            }
            if (selected != null) {
                AppBottomBar(
                    items = TopLevelDestination.entries.map {
                        AppBottomBarItem(key = it.name, label = stringResource(it.label), icon = it.icon)
                    },
                    selectedKey = selected.name,
                    onSelect = { key -> navController.navigateToTab(TopLevelDestination.valueOf(key)) },
                )
            }
        }
    }
}

private fun NavGraphBuilder.placeholder(tab: TopLevelDestination) {
    val content: @Composable () -> Unit = {
        EmptyView(
            title = stringResource(tab.emptyTitle),
            message = stringResource(tab.emptyMessage),
            modifier = Modifier.padding(AppTheme.spacing.screenPadding),
        )
    }
    when (tab.route) {
        Route.Kitchen -> composable<Route.Kitchen> { content() }
        Route.Pantry -> composable<Route.Pantry> { content() }
        Route.Bills -> composable<Route.Bills> { content() }
        Route.Balances -> composable<Route.Balances> { content() }
        Route.Catalog -> Unit
    }
}

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
