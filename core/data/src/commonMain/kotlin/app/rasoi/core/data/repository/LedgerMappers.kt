package app.rasoi.core.data.repository

import app.rasoi.core.contract.ledger.BalanceDto
import app.rasoi.core.contract.ledger.LedgerDto
import app.rasoi.core.contract.ledger.TransferDto
import app.rasoi.core.model.Balance
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.Money
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.Transfer
import app.rasoi.core.model.UserId

private fun month(text: String) = requireNotNull(PeriodMonth.parse(text)) { "Server sent a bad month: $text" }

internal fun LedgerDto.toModel() = Ledger(
    month = month(this.month),
    status = PeriodStatus.valueOf(status.name),
    balances = balances.map { it.toModel() },
    transfers = transfers.map { it.toModel() },
    pendingBills = pendingBills,
    months = months.map(::month),
)

private fun BalanceDto.toModel() = Balance(UserId(userId), Money(netPaise))

internal fun TransferDto.toModel() = LedgerTransfer(
    transfer = Transfer(UserId(from), UserId(to), Money(amountPaise)),
    settlementId = settlementId?.let(::SettlementId),
    status = status?.let { SettlementStatus.valueOf(it.name) },
    payeeUpiVpa = payeeUpiVpa,
)
