package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** A member's decision on a submitted bill; [automatic] marks the approval given after 48 hours of silence. */
@Serializable
data class ApprovalDto(
    val userId: String,
    val state: ApprovalStateDto,
    val comment: String? = null,
    val decidedAt: Instant? = null,
    val automatic: Boolean = false,
)
