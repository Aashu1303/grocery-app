package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

@Serializable
enum class PlatformDto { ZEPTO, BLINKIT, INSTAMART, HANDPICKD, MANUAL, OTHER }
