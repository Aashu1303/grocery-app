package app.rasoi.core.domain.ledger

import app.rasoi.core.model.Balance
import app.rasoi.core.model.Money
import app.rasoi.core.model.Transfer
import app.rasoi.core.model.UserId
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebtSimplifierTest {
    private fun balance(user: String, paise: Long) = Balance(UserId(user), Money(paise))

    private fun transfer(from: String, to: String, paise: Long) = Transfer(UserId(from), UserId(to), Money(paise))

    @Test
    fun oneDebtorPaysOneCreditor() {
        val result = DebtSimplifier.simplify(listOf(balance("a", 5_000), balance("b", -5_000)))
        assertEquals(listOf(transfer("b", "a", 5_000)), result)
    }

    @Test
    fun theLargestDebtorPaysTheLargestCreditorFirst() {
        val result = DebtSimplifier.simplify(
            listOf(balance("a", 7_000), balance("b", 3_000), balance("c", -6_000), balance("d", -4_000)),
        )
        assertEquals(
            listOf(transfer("c", "a", 6_000), transfer("d", "b", 3_000), transfer("d", "a", 1_000)),
            result,
        )
    }

    @Test
    fun anExactMatchNeedsOnlyOneTransfer() {
        val result = DebtSimplifier.simplify(
            listOf(balance("a", 4_000), balance("b", 1_000), balance("c", -1_000), balance("d", -4_000)),
        )
        assertEquals(listOf(transfer("d", "a", 4_000), transfer("c", "b", 1_000)), result)
    }

    @Test
    fun tiesAreBrokenByMemberId() {
        val result = DebtSimplifier.simplify(
            listOf(balance("b", 2_000), balance("a", 2_000), balance("d", -2_000), balance("c", -2_000)),
        )
        assertEquals(listOf(transfer("c", "a", 2_000), transfer("d", "b", 2_000)), result)
    }

    @Test
    fun settledMembersAndEmptyLedgersNeedNothing() {
        assertEquals(emptyList(), DebtSimplifier.simplify(emptyList()))
        assertEquals(emptyList(), DebtSimplifier.simplify(listOf(balance("a", 0), balance("b", 0))))
    }

    @Test
    fun balancesThatDoNotSumToZeroAreRefused() {
        val error = runCatching {
            DebtSimplifier.simplify(listOf(balance("a", 100), balance("b", -50)))
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun randomLedgersSettleCompletelyInAtMostNMinusOneTransfers() {
        val random = Random(99)
        repeat(500) {
            val size = random.nextInt(1, 9)
            val amounts = MutableList(size) { random.nextLong(-50_000, 50_000) }
            amounts[size - 1] = -amounts.take(size - 1).sum()
            val balances = amounts.mapIndexed { index, paise -> balance("m$index", paise) }
            val transfers = DebtSimplifier.simplify(balances)

            assertTrue(transfers.size <= maxOf(size - 1, 0), "${transfers.size} transfers for $size members")
            assertTrue(transfers.all { it.amount.paise > 0 && it.from != it.to })
            val settled = balances.associate { it.userId to it.net.paise }.toMutableMap()
            transfers.forEach {
                settled[it.from] = settled.getValue(it.from) + it.amount.paise
                settled[it.to] = settled.getValue(it.to) - it.amount.paise
            }
            assertTrue(settled.values.all { it == 0L }, "unsettled: $settled")
        }
    }
}
