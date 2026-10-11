package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A review warning; mismatches carry the computed and printed totals. */
@Serializable
data class BillWarningDto(
    val code: BillWarningCodeDto,
    val computedPaise: Long? = null,
    val printedPaise: Long? = null,
)
