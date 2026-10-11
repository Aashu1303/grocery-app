package app.rasoi.core.domain.split

import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Money
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LineAllocatorTest {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val lakshmi = UserId("u3")
    private val line = Builders.line(id = "l1", amount = 10_000, units = 3)

    private fun allocate(split: LineSplit, target: BillLine = line) = LineAllocator.allocate(target, split)

    private fun amounts(result: AppResult<List<Allocation>>) =
        assertIs<AppResult.Success<List<Allocation>>>(result).value.map { it.userId to it.amount.paise }

    private fun violation(result: AppResult<*>) =
        assertIs<AppError.Validation>(assertIs<AppResult.Failure>(result).error).violations.single().code

    @Test
    fun oneMemberPaysTheWholeLine() {
        val result = allocate(LineSplit(SplitMode.ONE, listOf(SplitEntry(aarav))))
        assertEquals(listOf(aarav to 10_000L), amounts(result))
    }

    @Test
    fun equalSharesAreExactToThePaise() {
        val split = LineSplit(SplitMode.EQUAL, listOf(SplitEntry(meera), SplitEntry(aarav), SplitEntry(lakshmi)))
        val shares = amounts(allocate(split))
        assertEquals(listOf(meera to 3_334L, aarav to 3_333L, lakshmi to 3_333L), shares)
        assertEquals(10_000L, shares.sumOf { it.second })
    }

    @Test
    fun quantitySplitFollowsTheUnitsEachMemberTook() {
        val split = LineSplit(SplitMode.QUANTITY, listOf(SplitEntry(meera, 1), SplitEntry(aarav, 2)))
        val result = assertIs<AppResult.Success<List<Allocation>>>(allocate(split)).value
        assertEquals(listOf(meera to 3_333L, aarav to 6_667L), result.map { it.userId to it.amount.paise })
        assertEquals(listOf(1, 2), result.map { it.units })
        assertEquals(SplitMode.QUANTITY, result.first().mode)
        assertEquals(BillLineId("l1"), result.first().lineId)
    }

    @Test
    fun aFreeLineAllocatesZeroToEveryone() {
        val free = Builders.line(id = "l2", amount = 0)
        val split = LineSplit(SplitMode.EQUAL, listOf(SplitEntry(meera), SplitEntry(aarav)))
        assertEquals(listOf(meera to 0L, aarav to 0L), amounts(allocate(split, free)))
    }

    @Test
    fun rejectsSplitsThatDoNotMatchTheirMode() {
        assertEquals("members_required", violation(allocate(LineSplit(SplitMode.EQUAL, emptyList()))))
        val two = listOf(SplitEntry(meera), SplitEntry(aarav))
        assertEquals("one_member", violation(allocate(LineSplit(SplitMode.ONE, two))))
        val twice = listOf(SplitEntry(meera), SplitEntry(meera))
        assertEquals("duplicate_member", violation(allocate(LineSplit(SplitMode.EQUAL, twice))))
    }

    @Test
    fun quantitySplitMustAccountForEveryUnitExactly() {
        assertEquals(
            "units_mismatch",
            violation(allocate(LineSplit(SplitMode.QUANTITY, listOf(SplitEntry(meera, 1), SplitEntry(aarav, 1))))),
        )
        assertEquals(
            "units_mismatch",
            violation(allocate(LineSplit(SplitMode.QUANTITY, listOf(SplitEntry(meera, 2), SplitEntry(aarav, 2))))),
        )
        assertEquals(
            "units_invalid",
            violation(allocate(LineSplit(SplitMode.QUANTITY, listOf(SplitEntry(meera, 3), SplitEntry(aarav, 0))))),
        )
    }

    @Test
    fun linesWithNegativeAmountsAreRejected() {
        val odd = line.copy(amount = Money(-100))
        assertEquals("amount_invalid", violation(allocate(LineSplit(SplitMode.ONE, listOf(SplitEntry(meera))), odd)))
    }
}
