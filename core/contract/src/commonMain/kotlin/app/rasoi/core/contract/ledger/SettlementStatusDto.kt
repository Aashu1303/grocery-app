package app.rasoi.core.contract.ledger

import kotlinx.serialization.Serializable

@Serializable
enum class SettlementStatusDto { PENDING, PAID }
