package app.rasoi.core.domain.split

import app.rasoi.core.model.Money
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LargestRemainderTest {
    private fun shares(total: Long, vararg weights: Long) =
        LargestRemainder.distribute(Money(total), weights.toList())?.map { it.paise }

    @Test
    fun splitsEvenlyWhenThePaiseDivide() {
        assertEquals(listOf(300L, 300L, 300L), shares(900, 1, 1, 1))
    }

    @Test
    fun handsLeftoverPaiseToTheLargestFractionalParts() {
        // 100 / 3 = 33.33: the one leftover paisa goes to the first share, as ties favour earlier positions.
        assertEquals(listOf(34L, 33L, 33L), shares(100, 1, 1, 1))
        // 10 * 1/3 and 10 * 2/3: floors 3 and 6 leave one paisa for the larger fraction (2/3 -> .67).
        assertEquals(listOf(3L, 7L), shares(10, 1, 2))
    }

    @Test
    fun followsTheWeights() {
        assertEquals(listOf(2_500L, 7_500L), shares(10_000, 1, 3))
        assertEquals(listOf(0L, 10_000L), shares(10_000, 0, 5))
    }

    @Test
    fun negativeTotalsSplitTheSameWayWithTheSignKept() {
        assertEquals(listOf(-34L, -33L, -33L), shares(-100, 1, 1, 1))
        assertEquals(listOf(0L, 0L), shares(0, 1, 1))
    }

    @Test
    fun rejectsWeightsThatCannotShareAnything() {
        assertNull(shares(100))
        assertNull(shares(100, 0, 0))
        assertNull(shares(100, 1, -1))
    }

    @Test
    fun rejectsProductsThatWouldOverflow() {
        assertNull(shares(Long.MAX_VALUE / 2, 3_000_000_000L, 3_000_000_000L))
    }

    @Test
    fun sharesAlwaysSumToTheTotalAndStayWithinAPaisaOfTheExactShare() {
        val random = Random(20261011)
        repeat(2_000) {
            val total = random.nextLong(-5_000_000_00, 5_000_000_00)
            val weights = List(random.nextInt(1, 9)) { random.nextLong(0, 1_000_000) }
            if (weights.sum() == 0L) return@repeat
            val result = assertNotNull(LargestRemainder.distribute(Money(total), weights))
            assertEquals(total, result.sumOf { it.paise }, "total=$total weights=$weights")
            val sum = weights.sum().toDouble()
            result.forEachIndexed { index, share ->
                val exact = total * weights[index] / sum
                assertTrue(kotlin.math.abs(share.paise - exact) < 1.0 + 1e-6, "share ${share.paise} vs $exact")
                if (weights[index] == 0L) assertEquals(0L, share.paise)
            }
        }
    }
}
