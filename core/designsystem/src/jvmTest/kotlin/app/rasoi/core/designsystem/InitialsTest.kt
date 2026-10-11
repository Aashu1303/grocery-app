package app.rasoi.core.designsystem

import app.rasoi.core.designsystem.component.initialsOf
import kotlin.test.Test
import kotlin.test.assertEquals

class InitialsTest {
    @Test
    fun takesUpToTwoInitials() {
        assertEquals("AS", initialsOf("Aarav Shah"))
        assertEquals("R", initialsOf("ravi"))
        assertEquals("MI", initialsOf("  Meera   Iyer Kumar "))
        assertEquals("", initialsOf("   "))
    }
}
