package app.rasoi.core.model

/** What a member is owed (positive) or owes (negative) over a period; a period's balances sum to zero. */
data class Balance(val userId: UserId, val net: Money)
