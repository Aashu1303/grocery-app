package app.rasoi.core.contract.ledger

import kotlinx.serialization.Serializable

/** What a member is owed (positive) or owes (negative) in the period. */
@Serializable
data class BalanceDto(val userId: String, val netPaise: Long)
