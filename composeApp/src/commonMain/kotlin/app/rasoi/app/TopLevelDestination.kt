package app.rasoi.app

import androidx.compose.ui.graphics.vector.ImageVector
import app.rasoi.app.resources.Res
import app.rasoi.app.resources.balances_empty_message
import app.rasoi.app.resources.balances_empty_title
import app.rasoi.app.resources.bills_empty_message
import app.rasoi.app.resources.bills_empty_title
import app.rasoi.app.resources.kitchen_empty_message
import app.rasoi.app.resources.kitchen_empty_title
import app.rasoi.app.resources.pantry_empty_message
import app.rasoi.app.resources.pantry_empty_title
import app.rasoi.app.resources.tab_balances
import app.rasoi.app.resources.tab_bills
import app.rasoi.app.resources.tab_kitchen
import app.rasoi.app.resources.tab_pantry
import app.rasoi.core.designsystem.icon.AppIcons
import org.jetbrains.compose.resources.StringResource

/** Bottom-bar destinations (design-plan 7.3); Kitchen is home. */
enum class TopLevelDestination(
    val route: Route,
    val label: StringResource,
    val icon: ImageVector,
    val emptyTitle: StringResource,
    val emptyMessage: StringResource,
) {
    KITCHEN(
        Route.Kitchen,
        Res.string.tab_kitchen,
        AppIcons.Kitchen,
        Res.string.kitchen_empty_title,
        Res.string.kitchen_empty_message,
    ),
    PANTRY(
        Route.Pantry,
        Res.string.tab_pantry,
        AppIcons.Pantry,
        Res.string.pantry_empty_title,
        Res.string.pantry_empty_message,
    ),
    BILLS(
        Route.Bills,
        Res.string.tab_bills,
        AppIcons.Bills,
        Res.string.bills_empty_title,
        Res.string.bills_empty_message,
    ),
    BALANCES(
        Route.Balances,
        Res.string.tab_balances,
        AppIcons.Balances,
        Res.string.balances_empty_title,
        Res.string.balances_empty_message,
    ),
}
