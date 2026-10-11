package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

@Serializable
enum class ApprovalStateDto { PENDING, APPROVED, CHANGES_REQUESTED }
