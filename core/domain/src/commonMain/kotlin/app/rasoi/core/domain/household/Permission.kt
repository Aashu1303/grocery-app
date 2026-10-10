package app.rasoi.core.domain.household

/** Actions guarded by household role (ADR 0014). */
enum class Permission {
    VIEW_HOUSEHOLD,
    INVITE_MEMBERS,
    MANAGE_LOCATIONS,
    VIEW_PANTRY,
    EDIT_PANTRY,
    VIEW_BILLS,
    EDIT_BILLS,
    APPROVE_BILLS,
    VIEW_LEDGER,
    SETTLE,
    CLOSE_PERIOD,
}
