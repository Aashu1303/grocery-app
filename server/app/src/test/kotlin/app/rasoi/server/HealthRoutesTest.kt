package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.HealthDto
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {
    @Test
    fun healthReportsOkAndVersion() = rasoiTest {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(HealthDto("ok", "test"), response.body())
    }

    @Test
    fun unknownRouteReturnsTypedNotFound() = rasoiTest {
        val response = client.get("/nope")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", response.body<ErrorDto>().code)
    }
}
