package app.rasoi.core.domain.household

import app.rasoi.core.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class RolePolicyTest {
    @Test
    fun ownerCanDoEverything() {
        val allowed = Permission.entries.filter { RolePolicy.allows(Role.OWNER, it) }.toSet()
        assertEquals(Permission.entries.toSet(), allowed)
    }

    @Test
    fun memberCannotManageTheHouseholdOrCloseMonths() {
        val denied = Permission.entries.filterNot { RolePolicy.allows(Role.MEMBER, it) }.toSet()
        assertEquals(setOf(Permission.INVITE_MEMBERS, Permission.CLOSE_PERIOD), denied)
    }

    @Test
    fun cookSeesTheKitchenButNotMoney() {
        val allowed = Permission.entries.filter { RolePolicy.allows(Role.COOK, it) }.toSet()
        assertEquals(setOf(Permission.VIEW_HOUSEHOLD, Permission.VIEW_PANTRY, Permission.EDIT_PANTRY), allowed)
    }
}
