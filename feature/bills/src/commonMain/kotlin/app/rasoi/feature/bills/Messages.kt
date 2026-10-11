package app.rasoi.feature.bills

import androidx.compose.runtime.Composable
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.Platform
import app.rasoi.feature.bills.resources.Res
import app.rasoi.feature.bills.resources.bills_date_format
import app.rasoi.feature.bills.resources.bills_months_short
import app.rasoi.feature.bills.resources.bills_status_approved
import app.rasoi.feature.bills.resources.bills_status_changes
import app.rasoi.feature.bills.resources.bills_status_draft
import app.rasoi.feature.bills.resources.bills_status_locked
import app.rasoi.feature.bills.resources.bills_status_pending
import app.rasoi.feature.bills.resources.error_changed
import app.rasoi.feature.bills.resources.error_forbidden
import app.rasoi.feature.bills.resources.error_generic
import app.rasoi.feature.bills.resources.error_locked
import app.rasoi.feature.bills.resources.error_mismatch
import app.rasoi.feature.bills.resources.error_not_found
import app.rasoi.feature.bills.resources.error_not_split
import app.rasoi.feature.bills.resources.error_offline
import app.rasoi.feature.bills.resources.field_invalid_amount
import app.rasoi.feature.bills.resources.field_required
import app.rasoi.feature.bills.resources.field_too_long
import app.rasoi.feature.bills.resources.platform_blinkit
import app.rasoi.feature.bills.resources.platform_handpickd
import app.rasoi.feature.bills.resources.platform_instamart
import app.rasoi.feature.bills.resources.platform_manual
import app.rasoi.feature.bills.resources.platform_other
import app.rasoi.feature.bills.resources.platform_zepto
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

internal val billsErrorMessages = mapOf(
    BillsError.OFFLINE to Res.string.error_offline,
    BillsError.FORBIDDEN to Res.string.error_forbidden,
    BillsError.NOT_FOUND to Res.string.error_not_found,
    BillsError.LOCKED to Res.string.error_locked,
    BillsError.CHANGED to Res.string.error_changed,
    BillsError.NOT_SPLIT to Res.string.error_not_split,
    BillsError.MISMATCH to Res.string.error_mismatch,
    BillsError.GENERIC to Res.string.error_generic,
)

@Composable
internal fun BillsError.text(): String = stringResource(billsErrorMessages.getValue(this))

@Composable
internal fun FieldError.text(): String = stringResource(
    when (this) {
        FieldError.REQUIRED -> Res.string.field_required
        FieldError.TOO_LONG -> Res.string.field_too_long
        FieldError.INVALID -> Res.string.field_invalid_amount
    },
)

@Composable
internal fun BillStatus.label(): String = stringResource(
    when (this) {
        BillStatus.DRAFT -> Res.string.bills_status_draft
        BillStatus.PENDING -> Res.string.bills_status_pending
        BillStatus.CHANGES_REQUESTED -> Res.string.bills_status_changes
        BillStatus.APPROVED -> Res.string.bills_status_approved
        BillStatus.LOCKED -> Res.string.bills_status_locked
    },
)

@Composable
internal fun Platform.label(): String = stringResource(
    when (this) {
        Platform.ZEPTO -> Res.string.platform_zepto
        Platform.BLINKIT -> Res.string.platform_blinkit
        Platform.INSTAMART -> Res.string.platform_instamart
        Platform.HANDPICKD -> Res.string.platform_handpickd
        Platform.MANUAL -> Res.string.platform_manual
        Platform.OTHER -> Res.string.platform_other
    },
)

/** "5 Oct 2026". */
@Composable
internal fun LocalDate.text(): String = stringResource(
    Res.string.bills_date_format,
    day,
    stringArrayResource(Res.array.bills_months_short)[month.ordinal],
    year,
)
