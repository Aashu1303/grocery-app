package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

@Serializable
enum class BillWarningCodeDto { TOTAL_MISMATCH, PARSED_BY_FALLBACK }
