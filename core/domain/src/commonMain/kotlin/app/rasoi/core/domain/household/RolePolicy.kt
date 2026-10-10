package app.rasoi.core.domain.household

import app.rasoi.core.model.Role

/** Which [Permission]s each [Role] holds. Owners hold all; cooks work with the kitchen but never see money. */
object RolePolicy {
    private val ownerOnly = setOf(Permission.INVITE_MEMBERS, Permission.CLOSE_PERIOD)
    private val cook = setOf(Permission.VIEW_HOUSEHOLD, Permission.VIEW_PANTRY, Permission.EDIT_PANTRY)

    fun allows(role: Role, permission: Permission): Boolean = when (role) {
        Role.OWNER -> true
        Role.MEMBER -> permission !in ownerOnly
        Role.COOK -> permission in cook
    }
}
