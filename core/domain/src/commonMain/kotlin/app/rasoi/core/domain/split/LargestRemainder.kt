package app.rasoi.core.domain.split

import app.rasoi.core.model.Money

/**
 * Design-plan 6.4 item 2: splits [Money] by integer weights so the shares always add up to the total. Each share is
 * the floor of its exact portion in paise; the paise left over go one each to the largest fractional parts, earlier
 * positions first on a tie.
 */
object LargestRemainder {
    /** Both the total and the weights must stay below this, so `total * weight` cannot overflow a Long. */
    private const val LIMIT = 3_000_000_000L

    /** Null when there is nothing to share by (no positive weight, a negative weight) or the numbers are too big. */
    fun distribute(total: Money, weights: List<Long>): List<Money>? {
        val weightSum = weights.sum()
        val magnitude = total.paise.let { if (it < 0) -it else it }
        val usable = weights.isNotEmpty() && weights.all { it >= 0 } && weightSum > 0
        if (!usable || maxOf(weightSum, magnitude) >= LIMIT) return null
        val exact = weights.map { magnitude * it }
        val floors = exact.map { it / weightSum }
        val leftover = (magnitude - floors.sum()).toInt()
        val extra = exact.indices
            .sortedWith(compareByDescending<Int> { exact[it] % weightSum }.thenBy { it })
            .take(leftover)
            .toSet()
        val sign = if (total.isNegative) -1L else 1L
        return floors.mapIndexed { index, floor -> Money(sign * (floor + if (index in extra) 1 else 0)) }
    }
}
