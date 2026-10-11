package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** What one member owes on a bill: their lines plus their part of its fees and discounts. */
@Serializable
data class MemberShareDto(val userId: String, val linesPaise: Long, val chargesPaise: Long, val totalPaise: Long)
