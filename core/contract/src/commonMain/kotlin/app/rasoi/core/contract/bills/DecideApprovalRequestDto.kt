package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** Approves the bill or asks for changes; a change request needs a [comment]. */
@Serializable
data class DecideApprovalRequestDto(val state: ApprovalStateDto, val comment: String? = null)
