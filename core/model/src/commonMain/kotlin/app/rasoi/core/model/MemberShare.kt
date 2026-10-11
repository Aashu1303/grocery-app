package app.rasoi.core.model

/** What one member owes on a bill: their lines plus their part of its fees and discounts. */
data class MemberShare(val userId: UserId, val lines: Money, val charges: Money) {
    val total: Money get() = lines + charges
}
