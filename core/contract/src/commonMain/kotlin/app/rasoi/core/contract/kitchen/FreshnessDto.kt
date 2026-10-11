package app.rasoi.core.contract.kitchen

import kotlinx.serialization.Serializable

@Serializable
enum class FreshnessDto { FRESH, USE_SOON, EXPIRED }
