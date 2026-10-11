package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

@Serializable
data class BillLineDto(
    val id: String,
    val rawName: String,
    val displayName: String,
    val units: Int,
    val amountPaise: Long,
    val measure: QuantityDto? = null,
    val mrpPaise: Long? = null,
    val hsn: String? = null,
    val upc: String? = null,
)
