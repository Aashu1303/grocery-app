package app.rasoi.feature.ledger

import androidx.compose.runtime.Composable
import app.rasoi.feature.ledger.resources.Res
import app.rasoi.feature.ledger.resources.error_changed
import app.rasoi.feature.ledger.resources.error_forbidden
import app.rasoi.feature.ledger.resources.error_generic
import app.rasoi.feature.ledger.resources.error_no_upi_app
import app.rasoi.feature.ledger.resources.error_not_found
import app.rasoi.feature.ledger.resources.error_offline
import org.jetbrains.compose.resources.stringResource

internal val ledgerErrorMessages = mapOf(
    LedgerError.OFFLINE to Res.string.error_offline,
    LedgerError.FORBIDDEN to Res.string.error_forbidden,
    LedgerError.NOT_FOUND to Res.string.error_not_found,
    LedgerError.CHANGED to Res.string.error_changed,
    LedgerError.NO_UPI_APP to Res.string.error_no_upi_app,
    LedgerError.GENERIC to Res.string.error_generic,
)

@Composable
internal fun LedgerError.text(): String = stringResource(ledgerErrorMessages.getValue(this))
