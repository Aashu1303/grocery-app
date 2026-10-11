package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

@Serializable
enum class ChargeKindDto { HANDLING, DELIVERY, DISCOUNT, ROUND_OFF, OTHER }
