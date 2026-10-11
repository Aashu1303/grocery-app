package app.rasoi.core.domain.ledger

import app.rasoi.core.model.Balance
import app.rasoi.core.model.Money
import app.rasoi.core.model.Transfer
import app.rasoi.core.model.UserId

/**
 * Design-plan 6.4 item 5: repeatedly matches the member owed the most with the member who owes the most, so n members
 * settle in at most n - 1 transfers. Ties go to the lower member id, which keeps the result stable.
 */
object DebtSimplifier {
    fun simplify(balances: List<Balance>): List<Transfer> {
        require(balances.sumOf { it.net.paise } == 0L) { "balances must sum to zero" }
        val creditors = balances.filter { it.net.paise > 0 }.associate { it.userId to it.net.paise }.toMutableMap()
        val debtors = balances.filter { it.net.paise < 0 }.associate { it.userId to -it.net.paise }.toMutableMap()
        val transfers = mutableListOf<Transfer>()
        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = largest(creditors)
            val debtor = largest(debtors)
            val amount = minOf(creditors.getValue(creditor), debtors.getValue(debtor))
            transfers += Transfer(debtor, creditor, Money(amount))
            reduce(creditors, creditor, amount)
            reduce(debtors, debtor, amount)
        }
        return transfers
    }

    private fun largest(open: Map<UserId, Long>): UserId = open.entries.sortedWith(
        compareByDescending<Map.Entry<UserId, Long>> {
            it.value
        }.thenBy { it.key.value },
    ).first().key

    private fun reduce(open: MutableMap<UserId, Long>, user: UserId, amount: Long) {
        val left = open.getValue(user) - amount
        if (left == 0L) open.remove(user) else open[user] = left
    }
}
