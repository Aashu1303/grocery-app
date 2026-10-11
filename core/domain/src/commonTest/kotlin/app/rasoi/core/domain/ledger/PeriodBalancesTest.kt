package app.rasoi.core.domain.ledger

import app.rasoi.core.domain.split.LineAllocator
import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Balance
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Money
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PeriodBalancesTest {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val lakshmi = UserId("u3")

    private fun allocated(line: BillLine, mode: SplitMode, users: List<UserId>): List<Allocation> {
        val split = LineSplit(mode, users.map { SplitEntry(it) })
        return assertIs<AppResult.Success<List<Allocation>>>(LineAllocator.allocate(line, split)).value
    }

    /** Meera paid 400 for rice (shared with Aarav) and milk (Aarav's alone). */
    private fun bill(id: String = "b1", payer: String = "u1", status: BillStatus = BillStatus.APPROVED): Bill {
        val rice = Builders.line("$id-a", "Rice", 30_000)
        val milk = Builders.line("$id-b", "Milk", 10_000)
        return Builders.bill(id = id, lines = listOf(rice, milk), status = status, payerId = payer).copy(
            allocations =
            allocated(rice, SplitMode.EQUAL, listOf(meera, aarav)) + allocated(milk, SplitMode.ONE, listOf(aarav)),
        )
    }

    private fun net(balances: List<Balance>) = balances.associate { it.userId to it.net.paise }

    @Test
    fun thePayerIsCreditedTheTotalAndEveryoneDebitedTheirShare() {
        val balances = net(PeriodBalances.of(listOf(bill())))
        // Meera paid 40,000 and owes 15,000; Aarav owes 25,000.
        assertEquals(mapOf(meera to 25_000L, aarav to -25_000L), balances)
    }

    @Test
    fun aPayerWithNoShareIsOwedEverything() {
        val balances = net(PeriodBalances.of(listOf(bill(payer = "u3"))))
        assertEquals(mapOf(lakshmi to 40_000L, meera to -15_000L, aarav to -25_000L), balances)
    }

    @Test
    fun onlyApprovedAndLockedBillsCount() {
        val bills = listOf(
            bill("b1", status = BillStatus.APPROVED),
            bill("b2", status = BillStatus.LOCKED),
            bill("b3", status = BillStatus.PENDING),
            bill("b4", status = BillStatus.DRAFT),
            bill("b5", status = BillStatus.CHANGES_REQUESTED),
        )
        assertEquals(mapOf(meera to 50_000L, aarav to -50_000L), net(PeriodBalances.of(bills)))
    }

    @Test
    fun chargesAreSharedLikeOnTheBill() {
        val rice = Builders.line("a", "Rice", 30_000)
        val milk = Builders.line("b", "Milk", 10_000)
        val withFee = Builders.bill(
            lines = listOf(rice, milk),
            charges = listOf(Builders.charge(amount = 4_000)),
            status = BillStatus.APPROVED,
        )
            .copy(
                allocations =
                allocated(rice, SplitMode.ONE, listOf(meera)) + allocated(milk, SplitMode.ONE, listOf(aarav)),
            )
        // Fee 4,000 in proportion to 30,000 : 10,000 gives Meera 3,000 and Aarav 1,000.
        assertEquals(mapOf(meera to 11_000L, aarav to -11_000L), net(PeriodBalances.of(listOf(withFee))))
    }

    @Test
    fun membersWhoNetToZeroAreKeptSoTheLedgerListsThem() {
        fun even(id: String, payer: String): Bill {
            val line = Builders.line("$id-a", "Gas", 10_000)
            return Builders.bill(id = id, lines = listOf(line), status = BillStatus.APPROVED, payerId = payer)
                .copy(allocations = allocated(line, SplitMode.EQUAL, listOf(meera, aarav)))
        }
        val zero = PeriodBalances.of(listOf(even("b1", "u1"), even("b2", "u2")))
        assertEquals(mapOf(meera to 0L, aarav to 0L), net(zero))
    }

    @Test
    fun noBillsMeansNoBalances() {
        assertEquals(emptyList(), PeriodBalances.of(emptyList()))
    }

    @Test
    fun balancesAlwaysSumToZeroOnRandomPeriods() {
        val random = Random(2026)
        val users = List(5) { UserId("m$it") }
        repeat(300) {
            val bills = List(random.nextInt(1, 6)) { index ->
                val lines = List(random.nextInt(1, 5)) { l ->
                    Builders.line("b$index-l$l", "Item", random.nextLong(1, 90_000), units = random.nextInt(1, 4))
                }
                val charges = List(random.nextInt(0, 3)) { c ->
                    Builders.charge("b$index-c$c", amount = random.nextLong(-3_000, 8_000))
                }
                val allocations = lines.flatMap { line ->
                    val chosen = users.shuffled(random).take(random.nextInt(1, users.size + 1))
                    allocated(line, SplitMode.EQUAL, chosen)
                }
                val payer = users.random(random)
                Builders.bill("b$index", lines, charges, status = BillStatus.APPROVED, payerId = payer.value)
                    .copy(allocations = allocations)
            }
            assertEquals(0L, PeriodBalances.of(bills).sumOf { it.net.paise })
        }
    }

    @Test
    fun theMoneySpentIsTheSumOfWhatEveryoneOwes() {
        val balances = PeriodBalances.of(listOf(bill()))
        assertEquals(Money(0), balances.fold(Money.ZERO) { sum, balance -> sum + balance.net })
    }
}
