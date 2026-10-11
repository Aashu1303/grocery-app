package app.rasoi.core.contract.ledger

import kotlinx.serialization.Serializable

/**
 * [from] pays [to]. In an open period this is a suggestion ([settlementId] and [status] are null); once the period is
 * closed it is a stored settlement. [payeeUpiVpa] is the receiver's UPI address, when they have given one.
 */
@Serializable
data class TransferDto(
    val from: String,
    val to: String,
    val amountPaise: Long,
    val settlementId: String? = null,
    val status: SettlementStatusDto? = null,
    val payeeUpiVpa: String? = null,
)
