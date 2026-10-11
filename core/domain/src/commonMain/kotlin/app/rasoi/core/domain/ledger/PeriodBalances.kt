package app.rasoi.core.domain.ledger

import app.rasoi.core.domain.split.BillShares
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Balance
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.Money
import app.rasoi.core.model.UserId

/**
 * Design-plan 6.4 item 4: for each approved bill the payer is credited the total and every member debited their share
 * (their items plus their part of the fees and discounts). Bills that are not approved yet do not count. Because each
 * bill's shares sum to its total, the balances always sum to zero.
 */
object PeriodBalances {
    private val counted = setOf(BillStatus.APPROVED, BillStatus.LOCKED)

    fun of(bills: List<Bill>): List<Balance> {
        val net = linkedMapOf<UserId, Money>()
        bills.filter { it.status in counted }.forEach { bill ->
            val shares = (BillShares.of(bill) as? AppResult.Success)?.value.orEmpty()
            net.merge(bill.payerId, bill.total)
            shares.forEach { net.merge(it.userId, -it.total) }
        }
        return net.map { (user, amount) -> Balance(user, amount) }
    }

    private fun MutableMap<UserId, Money>.merge(user: UserId, amount: Money) {
        this[user] = (this[user] ?: Money.ZERO) + amount
    }
}
