package app.rasoi.core.domain.split

import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.MemberShare
import app.rasoi.core.model.Money
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BillSharesTest {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val lakshmi = UserId("u3")

    private fun allocated(line: BillLine, mode: SplitMode, vararg members: Pair<UserId, Int>): List<Allocation> {
        val split = LineSplit(mode, members.map { (user, units) -> SplitEntry(user, units) })
        return assertIs<AppResult.Success<List<Allocation>>>(LineAllocator.allocate(line, split)).value
    }

    private fun charge(id: String, paise: Long, distribution: ChargeDistribution = ChargeDistribution.PROPORTIONAL) =
        BillCharge(ChargeId(id), ChargeKind.OTHER, id, Money(paise), distribution)

    private fun shares(bill: Bill) = assertIs<AppResult.Success<List<MemberShare>>>(BillShares.of(bill)).value

    private fun totals(bill: Bill) = shares(bill).associate { it.userId to it.total.paise }

    @Test
    fun linesAndProportionalChargesAddUpPerMember() {
        val rice = Builders.line("a", "Rice", 30_000)
        val milk = Builders.line("b", "Milk", 10_000)
        val bill = Builders.bill(
            lines = listOf(rice, milk),
            charges = listOf(charge("fee", 2_000), charge("promo", -400)),
        ).copy(allocations = allocated(rice, SplitMode.ONE, meera to 1) + allocated(milk, SplitMode.ONE, aarav to 1))

        val result = shares(bill)
        // Charges net 1,600 paise: 75% to Meera (1,200) and 25% to Aarav (400).
        assertEquals(listOf(meera to 31_200L, aarav to 10_400L), result.map { it.userId to it.total.paise })
        assertEquals(listOf(30_000L, 10_000L), result.map { it.lines.paise })
        assertEquals(listOf(1_200L, 400L), result.map { it.charges.paise })
    }

    @Test
    fun equalChargesIgnoreWhatEachMemberBought() {
        val rice = Builders.line("a", "Rice", 30_000)
        val milk = Builders.line("b", "Milk", 10_000)
        val bill = Builders.bill(
            lines = listOf(rice, milk),
            charges = listOf(charge("fee", 2_000, ChargeDistribution.EQUAL)),
        ).copy(allocations = allocated(rice, SplitMode.ONE, meera to 1) + allocated(milk, SplitMode.ONE, aarav to 1))
        assertEquals(mapOf(meera to 31_000L, aarav to 11_000L), totals(bill))
    }

    @Test
    fun aBillOfFreeLinesSharesItsChargesEquallyAmongParticipants() {
        val flyer = Builders.line("a", "Flyer", 0)
        val bill = Builders.bill(lines = listOf(flyer), charges = listOf(charge("fee", 101)))
            .copy(allocations = allocated(flyer, SplitMode.EQUAL, meera to 1, aarav to 1))
        assertEquals(mapOf(meera to 51L, aarav to 50L), totals(bill))
    }

    @Test
    fun everyLineNeedsItsAllocationsToSumToItsAmount() {
        val rice = Builders.line("a", "Rice", 30_000)
        val milk = Builders.line("b", "Milk", 10_000)
        val bill = Builders.bill(lines = listOf(rice, milk))
            .copy(allocations = allocated(rice, SplitMode.ONE, meera to 1))
        val error = assertIs<AppResult.Failure>(BillShares.of(bill)).error
        assertEquals(
            listOf("lines[1].split" to "missing"),
            assertIs<AppError.Validation>(error).violations.map {
                it.field to
                    it.code
            },
        )

        val short = bill.copy(
            allocations = allocated(rice, SplitMode.ONE, meera to 1) +
                allocated(milk, SplitMode.ONE, aarav to 1).map { it.copy(amount = Money(9_999)) },
        )
        val shortError = assertIs<AppError.Validation>(assertIs<AppResult.Failure>(BillShares.of(short)).error)
        assertEquals("lines[1].split" to "amount_mismatch", shortError.violations.single().let { it.field to it.code })
    }

    @Test
    fun sharesAlwaysSumToLinesPlusChargesOnRandomBills() {
        val random = Random(4242)
        val people = listOf(meera, aarav, lakshmi)
        repeat(500) {
            val lines = List(random.nextInt(1, 6)) { index ->
                Builders.line("l$index", "Item $index", random.nextLong(0, 90_000), random.nextInt(1, 5))
            }
            val charges = List(random.nextInt(0, 4)) { index ->
                charge("c$index", random.nextLong(-5_000, 5_000), ChargeDistribution.entries.random(random))
            }
            val allocations = lines.flatMap { line ->
                val chosen = people.shuffled(random).take(random.nextInt(1, 4))
                if (chosen.size == 1 && random.nextBoolean()) {
                    allocated(line, SplitMode.ONE, chosen.single() to 1)
                } else {
                    allocated(line, SplitMode.EQUAL, *chosen.map { it to 1 }.toTypedArray())
                }
            }
            val bill = Builders.bill(lines = lines, charges = charges).copy(allocations = allocations)
            val expected = lines.sumOf { it.amount.paise } + charges.sumOf { it.amount.paise }
            assertEquals(expected, shares(bill).sumOf { it.total.paise })
            assertEquals(lines.sumOf { it.amount.paise }, shares(bill).sumOf { it.lines.paise })
        }
    }
}
