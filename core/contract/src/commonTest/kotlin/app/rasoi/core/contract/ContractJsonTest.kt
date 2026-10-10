package app.rasoi.core.contract

import kotlin.test.Test
import kotlin.test.assertEquals

class ContractJsonTest {
    @Test
    fun errorRoundTripsWithFields() {
        val error = ErrorDto("validation_failed", "Invalid request", listOf(FieldErrorDto("name", "blank")))
        val json = ContractJson.encodeToString(ErrorDto.serializer(), error)
        assertEquals(error, ContractJson.decodeFromString(ErrorDto.serializer(), json))
    }

    @Test
    fun unknownKeysAreIgnoredForForwardCompatibility() {
        val json = """{"status":"ok","version":"1","extra":1}"""
        val decoded = ContractJson.decodeFromString(HealthDto.serializer(), json)
        assertEquals(HealthDto("ok", "1"), decoded)
    }

    @Test
    fun defaultsAreEncoded() {
        assertEquals(
            """{"code":"c","message":"m","fields":[]}""",
            ContractJson.encodeToString(ErrorDto.serializer(), ErrorDto("c", "m")),
        )
    }
}
