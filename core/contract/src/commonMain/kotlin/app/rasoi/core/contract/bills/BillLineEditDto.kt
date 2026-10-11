package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** A line after editing; [id] is null for a new line. */
@Serializable
data class BillLineEditDto(val id: String? = null, val displayName: String, val units: Int, val amountPaise: Long)
