package app.rasoi.core.contract

import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.auth.UserDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.RoleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HouseholdContractTest {
    @Test
    fun instantsSerialiseAsIso8601() {
        val invite = InviteDto("ABCD2345", "h1", RoleDto.COOK, Instant.parse("2026-10-18T10:00:00Z"))
        val json = ContractJson.encodeToString(InviteDto.serializer(), invite)
        val expected = """{"code":"ABCD2345","householdId":"h1","role":"COOK","expiresAt":"2026-10-18T10:00:00Z"}"""
        assertEquals(expected, json)
        assertEquals(invite, ContractJson.decodeFromString(InviteDto.serializer(), json))
    }

    @Test
    fun sessionRoundTripsWithOptionalUpi() {
        val session = SessionDto("t", Instant.parse("2026-11-10T00:00:00Z"), UserDto("u1", "Meera"))
        val json = ContractJson.encodeToString(SessionDto.serializer(), session)
        assertEquals(session, ContractJson.decodeFromString(SessionDto.serializer(), json))
    }
}
