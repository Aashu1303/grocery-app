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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.rasoi.app.resources.Res
import app.rasoi.app.resources.app_load_error
import app.rasoi.app.resources.app_name
import app.rasoi.app.resources.bills_empty_message
import app.rasoi.app.resources.bills_empty_title
import app.rasoi.app.resources.bills_import
import app.rasoi.app.resources.open_catalog
import app.rasoi.core.designsystem.catalog.CatalogScreen
import app.rasoi.core.designsystem.component.AppBottomBar
import app.rasoi.core.designsystem.component.AppBottomBarItem
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.MemberAvatar
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.BillId
import app.rasoi.feature.household.detail.HouseholdRoute
import app.rasoi.feature.household.setup.HouseholdSetupRoute
import app.rasoi.feature.household.signin.SignInRoute
import app.rasoi.feature.invoiceimport.review.BillReviewRoute
import app.rasoi.feature.invoiceimport.upload.ImportRoute
import app.rasoi.feature.pantry.PantryRoute
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Root composable: routes between sign-in, household setup and the main tabs. */
@Composable
fun RasoiApp(showCatalog: Boolean, viewModel: AppViewModel = koinViewModel()) {
    AppTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
        when (val current = state) {
            AppState.Starting -> LoadingView(modifier)
            AppState.SignedOut -> SignInRoute(modifier)
            is AppState.NeedsHousehold -> HouseholdSetupRoute(onHouseholdReady = {
                viewModel.refresh()
            }, modifier = modifier)
            AppState.LoadFailed -> ErrorView(stringResource(Res.string.app_load_error), viewModel::refresh, modifier)
            is AppState.Ready -> MainScreen(current, showCatalog, modifier)
        }
    }
}

@Composable
private fun MainScreen(
    state: AppState.Ready,
    showCatalog: Boolean,
    modifier: Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selected = TopLevelDestination.entries.firstOrNull { tab -> destination?.hasRoute(tab.route::class) == true }
    Column(modifier) {
        if (selected != null) {
            AppTopBar(title = stringResource(Res.string.app_name), actions = {
                if (showCatalog) {
                    AppButton(
                        text = stringResource(Res.string.open_catalog),
                        onClick = { navController.navigate(Route.Catalog) },
                        variant = AppButtonVariant.TEXT,
                    )
                }
                MemberAvatar(name = state.account.displayName, onClick = { navController.navigate(Route.Household) })
            })
        }
        NavHost(navController, startDestination = Route.Kitchen, modifier = Modifier.weight(1f)) {
            appDestinations(state, navController)
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

private fun NavGraphBuilder.appDestinations(state: AppState.Ready, navController: NavHostController) {
    TopLevelDestination.entries.forEach { tab -> placeholder(tab) }
    composable<Route.Pantry> {
        PantryRoute(householdId = state.household.household.id, myRole = state.household.role)
    }
    composable<Route.Bills> {
        BillsPlaceholder(state, onImport = { navController.navigate(Route.ImportInvoices) })
    }
    composable<Route.ImportInvoices> {
        ImportRoute(
            householdId = state.household.household.id,
            myRole = state.household.role,
            onBack = { navController.popBackStack() },
            onBillImported = { billId ->
                navController.navigate(Route.BillReview(billId.value)) { popUpTo<Route.Bills>() }
            },
        )
    }
    composable<Route.BillReview> { entry ->
        BillReviewRoute(
            billId = BillId(entry.toRoute<Route.BillReview>().billId),
            onDone = { navController.popBackStack() },
        )
    }
    composable<Route.Catalog> { CatalogScreen(onBack = { navController.popBackStack() }) }
    composable<Route.Household> {
        HouseholdRoute(
            householdId = state.household.household.id,
            myRole = state.household.role,
            onBack = { navController.popBackStack() },
        )
    }
}

/** The Bills list arrives with bill splitting (M4); until then this tab starts invoice import. */
@Composable
private fun BillsPlaceholder(state: AppState.Ready, onImport: () -> Unit) {
    val canImport = RolePolicy.allows(state.household.role, Permission.EDIT_BILLS)
    EmptyView(
        title = stringResource(Res.string.bills_empty_title),
        message = stringResource(Res.string.bills_empty_message),
        actionLabel = if (canImport) stringResource(Res.string.bills_import) else null,
        onAction = if (canImport) onImport else null,
        modifier = Modifier.padding(AppTheme.spacing.screenPadding),
    )
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
        Route.Balances -> composable<Route.Balances> { content() }
        Route.Pantry, Route.Bills, Route.Catalog, Route.Household, Route.ImportInvoices, is Route.BillReview -> Unit
    }
}

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
