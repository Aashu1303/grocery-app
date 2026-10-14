package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SweepRoutesTest {
    private val token = SweepToken("t".repeat(32))

    @Test
    fun theEndpointDoesNotExistWithoutAToken() = rasoiTest {
        assertEquals(HttpStatusCode.NotFound, client.post("/internal/sweeps").status)
    }

    @Test
    fun aMissingOrWrongTokenIsRejected() = rasoiTest(sweepToken = token) {
        val missing = client.post("/internal/sweeps")
        assertEquals(HttpStatusCode.Unauthorized, missing.status)
        assertEquals("unauthenticated", missing.body<ErrorDto>().code)
        val wrong = client.post("/internal/sweeps") { header(SweepToken.HEADER, "x".repeat(32)) }
        assertEquals(HttpStatusCode.Unauthorized, wrong.status)
    }

    @Test
    fun theRightTokenRunsTheSweeps() = rasoiTest(sweepToken = token) {
        val response = client.post("/internal/sweeps") { header(SweepToken.HEADER, token.value) }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun aShortTokenIsRefusedAndTheTokenIsNeverPrinted() {
        assertFailsWith<IllegalArgumentException> { SweepToken("short") }
        assertFalse(token.toString().contains(token.value))
    }
}
