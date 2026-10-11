package app.rasoi.core.model

import kotlin.time.Instant

/**
 * One member's decision on a submitted bill (design-plan 6.3). [decidedAt] is null while PENDING; [automatic] marks
 * an approval the system gave after 48 hours without an answer.
 */
data class Approval(
    val userId: UserId,
    val state: ApprovalState,
    val comment: String? = null,
    val decidedAt: Instant? = null,
    val automatic: Boolean = false,
)
