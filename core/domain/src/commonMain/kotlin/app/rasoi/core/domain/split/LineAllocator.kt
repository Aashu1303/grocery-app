package app.rasoi.core.domain.split

import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.SplitMode

/**
 * Design-plan 6.4 item 1: turns a [LineSplit] into stored allocations that sum to the line's amount.
 * ONE gives the whole amount to one member, EQUAL splits it between the chosen members, and QUANTITY splits by
 * the units each member took, which must add up to the line's units.
 */
object LineAllocator {
    fun allocate(line: BillLine, split: LineSplit): AppResult<List<Allocation>> {
        val problem = problem(line, split)
        if (problem != null) return fail(problem)
        val weights = split.entries.map { if (split.mode == SplitMode.QUANTITY) it.units.toLong() else 1L }
        val shares = LargestRemainder.distribute(line.amount, weights) ?: return fail("amount_invalid")
        return AppResult.Success(
            split.entries.mapIndexed { index, entry ->
                Allocation(line.id, entry.userId, split.mode, entry.units, shares[index])
            },
        )
    }

    private fun problem(line: BillLine, split: LineSplit): String? = when {
        line.amount.isNegative -> "amount_invalid"
        split.entries.isEmpty() -> "members_required"
        split.mode == SplitMode.ONE && split.entries.size != 1 -> "one_member"
        split.entries.map { it.userId }.toSet().size != split.entries.size -> "duplicate_member"
        split.mode == SplitMode.QUANTITY -> unitsProblem(line, split)
        else -> null
    }

    private fun unitsProblem(line: BillLine, split: LineSplit): String? = when {
        split.entries.any { it.units <= 0 } -> "units_invalid"
        split.entries.sumOf { it.units } != line.units -> "units_mismatch"
        else -> null
    }

    private fun fail(code: String): AppResult<Nothing> =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("split", code))))
}
