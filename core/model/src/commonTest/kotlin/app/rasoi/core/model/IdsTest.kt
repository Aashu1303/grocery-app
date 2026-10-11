package app.rasoi.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IdsTest {
    @Test
    fun blankIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> { UserId(" ") }
        assertFailsWith<IllegalArgumentException> { HouseholdId("") }
        assertFailsWith<IllegalArgumentException> { BillId("") }
        assertFailsWith<IllegalArgumentException> { BillLineId("") }
        assertFailsWith<IllegalArgumentException> { ChargeId("") }
        assertFailsWith<IllegalArgumentException> { LocationId("") }
        assertFailsWith<IllegalArgumentException> { PantryItemId("") }
        assertFailsWith<IllegalArgumentException> { PeriodId("") }
        assertFailsWith<IllegalArgumentException> { SettlementId("") }
        assertFailsWith<IllegalArgumentException> { InviteCode("") }
    }

    @Test
    fun idsCompareByValue() {
        assertEquals(UserId("u1"), UserId("u1"))
        assertEquals("u1", UserId("u1").value)
    }
}
