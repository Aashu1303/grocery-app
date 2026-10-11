package app.rasoi.feature.invoiceimport

import androidx.compose.runtime.Composable
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Platform
import app.rasoi.feature.invoiceimport.resources.Res
import app.rasoi.feature.invoiceimport.resources.charge_delivery
import app.rasoi.feature.invoiceimport.resources.charge_discount
import app.rasoi.feature.invoiceimport.resources.charge_handling
import app.rasoi.feature.invoiceimport.resources.charge_other
import app.rasoi.feature.invoiceimport.resources.charge_round_off
import app.rasoi.feature.invoiceimport.resources.error_different_orders
import app.rasoi.feature.invoiceimport.resources.error_duplicate
import app.rasoi.feature.invoiceimport.resources.error_encrypted
import app.rasoi.feature.invoiceimport.resources.error_forbidden
import app.rasoi.feature.invoiceimport.resources.error_generic
import app.rasoi.feature.invoiceimport.resources.error_no_text
import app.rasoi.feature.invoiceimport.resources.error_not_a_pdf
import app.rasoi.feature.invoiceimport.resources.error_not_editable
import app.rasoi.feature.invoiceimport.resources.error_not_found
import app.rasoi.feature.invoiceimport.resources.error_offline
import app.rasoi.feature.invoiceimport.resources.error_repeated
import app.rasoi.feature.invoiceimport.resources.error_too_large
import app.rasoi.feature.invoiceimport.resources.error_too_many
import app.rasoi.feature.invoiceimport.resources.error_unsupported
import app.rasoi.feature.invoiceimport.resources.platform_blinkit
import app.rasoi.feature.invoiceimport.resources.platform_handpickd
import app.rasoi.feature.invoiceimport.resources.platform_instamart
import app.rasoi.feature.invoiceimport.resources.platform_manual
import app.rasoi.feature.invoiceimport.resources.platform_other
import app.rasoi.feature.invoiceimport.resources.platform_zepto
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val billErrorMessages: Map<BillError, StringResource> = mapOf(
    BillError.OFFLINE to Res.string.error_offline,
    BillError.FORBIDDEN to Res.string.error_forbidden,
    BillError.NOT_FOUND to Res.string.error_not_found,
    BillError.NOT_EDITABLE to Res.string.error_not_editable,
    BillError.DUPLICATE to Res.string.error_duplicate,
    BillError.NOT_A_PDF to Res.string.error_not_a_pdf,
    BillError.ENCRYPTED to Res.string.error_encrypted,
    BillError.NO_TEXT to Res.string.error_no_text,
    BillError.UNSUPPORTED to Res.string.error_unsupported,
    BillError.DIFFERENT_ORDERS to Res.string.error_different_orders,
    BillError.REPEATED to Res.string.error_repeated,
    BillError.TOO_LARGE to Res.string.error_too_large,
    BillError.TOO_MANY to Res.string.error_too_many,
    BillError.GENERIC to Res.string.error_generic,
)

@Composable
internal fun BillError.text(): String = stringResource(billErrorMessages.getValue(this))

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

@Composable
internal fun ChargeKind.label(): String = stringResource(
    when (this) {
        ChargeKind.HANDLING -> Res.string.charge_handling
        ChargeKind.DELIVERY -> Res.string.charge_delivery
        ChargeKind.DISCOUNT -> Res.string.charge_discount
        ChargeKind.ROUND_OFF -> Res.string.charge_round_off
        ChargeKind.OTHER -> Res.string.charge_other
    },
)
