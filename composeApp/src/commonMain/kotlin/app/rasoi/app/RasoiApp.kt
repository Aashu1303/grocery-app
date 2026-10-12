package app.rasoi.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
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
import app.rasoi.app.resources.open_catalog
import app.rasoi.core.designsystem.catalog.CatalogScreen
import app.rasoi.core.designsystem.component.AppBottomBar
import app.rasoi.core.designsystem.component.AppBottomBarItem
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppScaffold
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.MemberAvatar
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.designsystem.theme.FadeThrough
import app.rasoi.core.model.BillId
import app.rasoi.feature.bills.detail.BillDetailRoute
import app.rasoi.feature.bills.list.BillsRoute
import app.rasoi.feature.bills.list.BillsViewModel
import app.rasoi.feature.bills.list.billsViewModel
import app.rasoi.feature.household.detail.HouseholdRoute
import app.rasoi.feature.household.setup.HouseholdSetupRoute
import app.rasoi.feature.household.signin.SignInRoute
import app.rasoi.feature.invoiceimport.review.BillReviewRoute
import app.rasoi.feature.invoiceimport.upload.ImportRoute
import app.rasoi.feature.kitchen.KitchenRoute
import app.rasoi.feature.kitchen.KitchenViewModel
import app.rasoi.feature.kitchen.kitchenViewModel
import app.rasoi.feature.ledger.LedgerRoute
import app.rasoi.feature.ledger.LedgerViewModel
import app.rasoi.feature.ledger.ledgerViewModel
import app.rasoi.feature.pantry.PantryRoute
import app.rasoi.feature.pantry.PantryViewModel
import app.rasoi.feature.pantry.pantryViewModel
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
    val tabs = rememberTabViewModels(state)
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    // Remembered so the highlight stays on the last tab while the bar fades out over a detail screen.
    var selectedTab by remember { mutableStateOf(TopLevelDestination.KITCHEN) }
    TopLevelDestination.entries.firstOrNull { destination?.hasRoute(it.route::class) == true }?.let { selectedTab = it }
    AppScaffold(
        // The destination is null only on the first frame; counting it as a tab keeps the bars from fading in at
        // launch.
        barsVisible = destination == null || destination.isTab(),
        topBar = {
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
        },
        bottomBar = {
            AppBottomBar(
                items = TopLevelDestination.entries.map {
                    AppBottomBarItem(key = it.name, label = stringResource(it.label), icon = it.icon)
                },
                selectedKey = selectedTab.name,
                onSelect = { key -> navController.navigateToTab(TopLevelDestination.valueOf(key)) },
            )
        },
        modifier = modifier,
    ) { barsPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Kitchen,
            modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface),
            enterTransition = { FadeThrough.enter },
            exitTransition = { FadeThrough.exit },
            popEnterTransition = { FadeThrough.enter },
            popExitTransition = { FadeThrough.exit },
        ) {
            tabDestinations(state, tabs, Modifier.padding(barsPadding), navController)
            detailDestinations(state, navController)
        }
    }
}

/** The four tab ViewModels, built together so every tab has loaded before its first visit. */
private class TabViewModels(
    val kitchen: KitchenViewModel,
    val pantry: PantryViewModel,
    val bills: BillsViewModel,
    val ledger: LedgerViewModel,
)

/**
 * Created once for the signed-in household instead of per tab destination, so switching to a tab shows content that
 * is already loaded rather than a skeleton. The key includes the user and role so a new sign-in gets fresh ones.
 */
@Composable
private fun rememberTabViewModels(state: AppState.Ready): TabViewModels {
    val household = state.household.household.id
    val role = state.household.role
    val me = state.account.id
    val scope = "${me.value}:${household.value}:${role.name}"
    // A ViewModelStore is keyed by this string alone; sharing one key would let each ViewModel replace the last.
    return TabViewModels(
        kitchen = kitchenViewModel(household, role, key = "kitchen:$scope"),
        pantry = pantryViewModel(household, role, key = "pantry:$scope"),
        bills = billsViewModel(household, role, key = "bills:$scope"),
        ledger = ledgerViewModel(household, me, role, key = "ledger:$scope"),
    )
}

private fun NavDestination.isTab() = TopLevelDestination.entries.any { hasRoute(it.route::class) }

/** Tab screens sit between the bars, so they take [tabModifier]. */
private fun NavGraphBuilder.tabDestinations(
    state: AppState.Ready,
    tabs: TabViewModels,
    tabModifier: Modifier,
    navController: NavHostController,
) {
    composable<Route.Kitchen> {
        KitchenRoute(
            householdId = state.household.household.id,
            myRole = state.household.role,
            modifier = tabModifier,
            viewModel = tabs.kitchen,
        )
    }
    composable<Route.Pantry> {
        PantryRoute(
            householdId = state.household.household.id,
            myRole = state.household.role,
            modifier = tabModifier,
            viewModel = tabs.pantry,
        )
    }
    composable<Route.Bills> {
        BillsRoute(
            householdId = state.household.household.id,
            myRole = state.household.role,
            onOpenBill = { billId -> navController.navigate(Route.BillDetail(billId.value)) },
            onImport = { navController.navigate(Route.ImportInvoices) },
            modifier = tabModifier,
            viewModel = tabs.bills,
        )
    }
    composable<Route.Balances> {
        LedgerRoute(
            householdId = state.household.household.id,
            myUserId = state.account.id,
            myRole = state.household.role,
            modifier = tabModifier,
            viewModel = tabs.ledger,
        )
    }
}

/** Screens opened from a tab; the bars are hidden, so each fills the space with its own top bar. */
private fun NavGraphBuilder.detailDestinations(state: AppState.Ready, navController: NavHostController) {
    composable<Route.BillDetail> { entry ->
        BillDetailRoute(
            billId = BillId(entry.toRoute<Route.BillDetail>().billId),
            myUserId = state.account.id,
            myRole = state.household.role,
            onBack = { navController.popBackStack() },
            onEditItems = { billId -> navController.navigate(Route.BillReview(billId.value)) },
        )
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

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
