package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

@Serializable
enum class BillStatusDto { DRAFT, PENDING, CHANGES_REQUESTED, APPROVED, LOCKED }
