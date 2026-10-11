package app.rasoi.core.model

/** Bill lifecycle (design-plan 6.3): a draft is private to its uploader until submitted. */
enum class BillStatus { DRAFT, PENDING, CHANGES_REQUESTED, APPROVED, LOCKED }
