package app.rasoi.feature.household

import androidx.compose.runtime.Composable
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.feature.household.resources.Res
import app.rasoi.feature.household.resources.error_already_member
import app.rasoi.feature.household.resources.error_code_incorrect
import app.rasoi.feature.household.resources.error_forbidden
import app.rasoi.feature.household.resources.error_generic
import app.rasoi.feature.household.resources.error_invite_expired
import app.rasoi.feature.household.resources.error_invite_not_found
import app.rasoi.feature.household.resources.error_invite_used
import app.rasoi.feature.household.resources.error_location_exists
import app.rasoi.feature.household.resources.error_offline
import app.rasoi.feature.household.resources.error_required
import app.rasoi.feature.household.resources.error_too_long
import app.rasoi.feature.household.resources.kind_freezer
import app.rasoi.feature.household.resources.kind_fridge
import app.rasoi.feature.household.resources.kind_other
import app.rasoi.feature.household.resources.kind_shelf
import app.rasoi.feature.household.resources.role_cook
import app.rasoi.feature.household.resources.role_member
import app.rasoi.feature.household.resources.role_owner
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FieldError.text(invalid: StringResource): String = stringResource(
    when (this) {
        FieldError.INVALID -> invalid
        FieldError.INCORRECT -> Res.string.error_code_incorrect
        FieldError.REQUIRED -> Res.string.error_required
        FieldError.TOO_LONG -> Res.string.error_too_long
    },
)

@Composable
internal fun ScreenError.text(): String = stringResource(
    when (this) {
        ScreenError.OFFLINE -> Res.string.error_offline
        ScreenError.FORBIDDEN -> Res.string.error_forbidden
        ScreenError.INVITE_USED -> Res.string.error_invite_used
        ScreenError.INVITE_EXPIRED -> Res.string.error_invite_expired
        ScreenError.INVITE_NOT_FOUND -> Res.string.error_invite_not_found
        ScreenError.ALREADY_MEMBER -> Res.string.error_already_member
        ScreenError.DUPLICATE -> Res.string.error_location_exists
        ScreenError.GENERIC -> Res.string.error_generic
    },
)

@Composable
internal fun Role.label(): String = stringResource(
    when (this) {
        Role.OWNER -> Res.string.role_owner
        Role.MEMBER -> Res.string.role_member
        Role.COOK -> Res.string.role_cook
    },
)

@Composable
internal fun LocationKind.label(): String = stringResource(
    when (this) {
        LocationKind.FRIDGE -> Res.string.kind_fridge
        LocationKind.FREEZER -> Res.string.kind_freezer
        LocationKind.SHELF -> Res.string.kind_shelf
        LocationKind.OTHER -> Res.string.kind_other
    },
)
