package app.rasoi.core.domain.split

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.MemberShare
import app.rasoi.core.model.Money
import app.rasoi.core.model.UserId
import app.rasoi.core.model.sum

/**
 * Design-plan 6.4 items 1 to 3: what each member owes on a bill. Lines come from the stored allocations; every fee
 * and discount is shared by largest remainder, in proportion to each member's lines (the default) or equally.
 * The shares always sum to the lines plus the charges.
 */
object BillShares {
    fun of(bill: Bill): AppResult<List<MemberShare>> {
        val missing = bill.lines.withIndex().mapNotNull { (index, line) ->
            val allocated = bill.allocations.filter { it.lineId == line.id }
            when {
                allocated.isEmpty() -> AppError.FieldViolation("lines[$index].split", "missing")
                allocated.map { it.amount }.sum() != line.amount ->
                    AppError.FieldViolation("lines[$index].split", "amount_mismatch")
                else -> null
            }
        }
        if (missing.isNotEmpty()) return AppResult.Failure(AppError.Validation(missing))
        val members = bill.allocations.map { it.userId }.distinct()
        val lineTotals = members.associateWith { user ->
            bill.allocations.filter { it.userId == user }.map { it.amount }.sum()
        }
        val chargeTotals = bill.charges.map { distribute(it, lineTotals) }
        return AppResult.Success(
            members.map { user ->
                MemberShare(user, lineTotals.getValue(user), chargeTotals.map { it.getValue(user) }.sum())
            },
        )
    }

    /** Proportional shares fall back to equal ones when nobody's lines cost anything. */
    private fun distribute(charge: BillCharge, lineTotals: Map<UserId, Money>): Map<UserId, Money> {
        val members = lineTotals.keys.toList()
        val proportional =
            charge.distribution == ChargeDistribution.PROPORTIONAL && lineTotals.values.any { !it.isZero }
        val weights = members.map { if (proportional) lineTotals.getValue(it).paise else 1L }
        val shares = checkNotNull(LargestRemainder.distribute(charge.amount, weights)) { "Charge cannot be shared" }
        return members.zip(shares).toMap()
    }
}
