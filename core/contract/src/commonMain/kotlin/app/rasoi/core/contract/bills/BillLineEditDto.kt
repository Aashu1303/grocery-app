package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A line after editing; [id] is null for a new line and [isFood] null keeps the current setting. */
@Serializable
data class BillLineEditDto(
    val id: String? = null,
    val displayName: String,
    val units: Int,
    val amountPaise: Long,
    val isFood: Boolean? = null,
)
