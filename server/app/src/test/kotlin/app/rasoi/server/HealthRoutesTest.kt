package app.rasoi.server

import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.HealthDto
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {
    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json(ContractJson) }
    }

    @Test
    fun healthReportsOkAndVersion() = testApplication {
        application { rasoiModule(ServerConfig(port = 0, version = "test")) }
        val response = jsonClient().get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(HealthDto("ok", "test"), response.body())
    }

    @Test
    fun unknownRouteReturnsTypedNotFound() = testApplication {
        application { rasoiModule(ServerConfig(port = 0, version = "test")) }
        val response = jsonClient().get("/nope")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", response.body<ErrorDto>().code)
    }
}
