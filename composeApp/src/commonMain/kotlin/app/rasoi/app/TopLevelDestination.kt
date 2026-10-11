package app.rasoi.app

import androidx.compose.ui.graphics.vector.ImageVector
import app.rasoi.app.resources.Res
import app.rasoi.app.resources.tab_balances
import app.rasoi.app.resources.tab_bills
import app.rasoi.app.resources.tab_kitchen
import app.rasoi.app.resources.tab_pantry
import app.rasoi.core.designsystem.icon.AppIcons
import org.jetbrains.compose.resources.StringResource

/** Bottom-bar destinations (design-plan 7.3); Kitchen is home. */
enum class TopLevelDestination(val route: Route, val label: StringResource, val icon: ImageVector) {
    KITCHEN(
        Route.Kitchen,
        Res.string.tab_kitchen,
        AppIcons.Kitchen,
    ),
    PANTRY(
        Route.Pantry,
        Res.string.tab_pantry,
        AppIcons.Pantry,
    ),
    BILLS(
        Route.Bills,
        Res.string.tab_bills,
        AppIcons.Bills,
    ),
    BALANCES(
        Route.Balances,
        Res.string.tab_balances,
        AppIcons.Balances,
    ),
}
