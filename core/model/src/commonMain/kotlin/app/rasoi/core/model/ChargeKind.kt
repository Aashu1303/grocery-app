package app.rasoi.core.model

/** A bill-level charge or discount; discounts carry negative amounts (design-plan 6.2). */
enum class ChargeKind { HANDLING, DELIVERY, DISCOUNT, ROUND_OFF, OTHER }
