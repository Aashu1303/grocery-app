package app.rasoi.feature.pantry

import androidx.compose.runtime.Composable
import app.rasoi.core.domain.pantry.ExpiryRules
import app.rasoi.core.domain.pantry.InputUnit
import app.rasoi.core.model.PantryItem
import app.rasoi.feature.pantry.resources.Res
import app.rasoi.feature.pantry.resources.error_forbidden
import app.rasoi.feature.pantry.resources.error_generic
import app.rasoi.feature.pantry.resources.error_not_found
import app.rasoi.feature.pantry.resources.error_offline
import app.rasoi.feature.pantry.resources.field_invalid_quantity
import app.rasoi.feature.pantry.resources.field_required
import app.rasoi.feature.pantry.resources.field_too_long
import app.rasoi.feature.pantry.resources.months_short
import app.rasoi.feature.pantry.resources.pantry_date_format
import app.rasoi.feature.pantry.resources.pantry_expired_days
import app.rasoi.feature.pantry.resources.pantry_expired_one
import app.rasoi.feature.pantry.resources.pantry_expiry_days
import app.rasoi.feature.pantry.resources.pantry_expiry_none
import app.rasoi.feature.pantry.resources.pantry_expiry_one
import app.rasoi.feature.pantry.resources.pantry_expiry_today
import app.rasoi.feature.pantry.resources.unit_grams
import app.rasoi.feature.pantry.resources.unit_kilograms
import app.rasoi.feature.pantry.resources.unit_litres
import app.rasoi.feature.pantry.resources.unit_millilitres
import app.rasoi.feature.pantry.resources.unit_pieces
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

internal val pantryErrorMessages = mapOf(
    PantryError.OFFLINE to Res.string.error_offline,
    PantryError.FORBIDDEN to Res.string.error_forbidden,
    PantryError.NOT_FOUND to Res.string.error_not_found,
    PantryError.GENERIC to Res.string.error_generic,
)

@Composable
internal fun PantryError.text(): String = stringResource(pantryErrorMessages.getValue(this))

@Composable
internal fun PantryFieldError.text(): String = stringResource(
    when (this) {
        PantryFieldError.REQUIRED -> Res.string.field_required
        PantryFieldError.TOO_LONG -> Res.string.field_too_long
        PantryFieldError.INVALID -> Res.string.field_invalid_quantity
    },
)

@Composable
internal fun InputUnit.label(): String = stringResource(
    when (this) {
        InputUnit.PIECES -> Res.string.unit_pieces
        InputUnit.GRAMS -> Res.string.unit_grams
        InputUnit.KILOGRAMS -> Res.string.unit_kilograms
        InputUnit.MILLILITRES -> Res.string.unit_millilitres
        InputUnit.LITRES -> Res.string.unit_litres
    },
)

/** "2 days left", "Expires today", "Expired yesterday" or "No expiry". */
@Composable
internal fun PantryItem.expiryText(today: LocalDate): String {
    val expiry = expiresOn ?: return stringResource(Res.string.pantry_expiry_none)
    val days = ExpiryRules.daysLeft(expiry, today)
    return when {
        days == 0 -> stringResource(Res.string.pantry_expiry_today)
        days == 1 -> stringResource(Res.string.pantry_expiry_one)
        days > 1 -> stringResource(Res.string.pantry_expiry_days, days)
        days == -1 -> stringResource(Res.string.pantry_expired_one)
        else -> stringResource(Res.string.pantry_expired_days, -days)
    }
}

/** "12 Oct". */
@Composable
internal fun LocalDate.shortText(): String =
    stringResource(Res.string.pantry_date_format, day, stringArrayResource(Res.array.months_short)[month.ordinal])
