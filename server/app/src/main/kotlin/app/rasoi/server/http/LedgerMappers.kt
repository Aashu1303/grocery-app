package app.rasoi.server.http

import app.rasoi.core.contract.ledger.BalanceDto
import app.rasoi.core.contract.ledger.LedgerDto
import app.rasoi.core.contract.ledger.PeriodStatusDto
import app.rasoi.core.contract.ledger.SettlementStatusDto
import app.rasoi.core.contract.ledger.TransferDto
import app.rasoi.core.model.Balance
import app.rasoi.server.service.Ledger
import app.rasoi.server.service.LedgerTransfer

internal fun Ledger.toDto() = LedgerDto(
    month = period.month.toString(),
    status = PeriodStatusDto.valueOf(period.status.name),
    balances = balances.map { it.toDto() },
    transfers = transfers.map { it.toDto() },
    pendingBills = pendingBills,
    months = months.map { it.toString() },
)

internal fun Balance.toDto() = BalanceDto(userId.value, net.paise)

internal fun LedgerTransfer.toDto() = TransferDto(
    from = transfer.from.value,
    to = transfer.to.value,
    amountPaise = transfer.amount.paise,
    settlementId = settlementId?.value,
    status = status?.let { SettlementStatusDto.valueOf(it.name) },
    payeeUpiVpa = payeeUpiVpa,
)
