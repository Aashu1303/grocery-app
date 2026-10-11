package app.rasoi.feature.kitchen

import androidx.compose.runtime.Composable
import app.rasoi.feature.kitchen.resources.Res
import app.rasoi.feature.kitchen.resources.error_forbidden
import app.rasoi.feature.kitchen.resources.error_generic
import app.rasoi.feature.kitchen.resources.error_ideas_failed
import app.rasoi.feature.kitchen.resources.error_offline
import app.rasoi.feature.kitchen.resources.kitchen_expired_days
import app.rasoi.feature.kitchen.resources.kitchen_expired_one
import app.rasoi.feature.kitchen.resources.kitchen_expiry_days
import app.rasoi.feature.kitchen.resources.kitchen_expiry_none
import app.rasoi.feature.kitchen.resources.kitchen_expiry_one
import app.rasoi.feature.kitchen.resources.kitchen_expiry_today
import org.jetbrains.compose.resources.stringResource

internal val kitchenErrorMessages = mapOf(
    KitchenError.OFFLINE to Res.string.error_offline,
    KitchenError.FORBIDDEN to Res.string.error_forbidden,
    KitchenError.IDEAS_FAILED to Res.string.error_ideas_failed,
    KitchenError.GENERIC to Res.string.error_generic,
)

@Composable
internal fun KitchenError.text(): String = stringResource(kitchenErrorMessages.getValue(this))

/** "2 days left", "Expires today", "Expired yesterday" or "No expiry". */
@Composable
internal fun expiryText(daysLeft: Int?): String = when {
    daysLeft == null -> stringResource(Res.string.kitchen_expiry_none)
    daysLeft == 0 -> stringResource(Res.string.kitchen_expiry_today)
    daysLeft == 1 -> stringResource(Res.string.kitchen_expiry_one)
    daysLeft > 1 -> stringResource(Res.string.kitchen_expiry_days, daysLeft)
    daysLeft == -1 -> stringResource(Res.string.kitchen_expired_one)
    else -> stringResource(Res.string.kitchen_expired_days, -daysLeft)
}
