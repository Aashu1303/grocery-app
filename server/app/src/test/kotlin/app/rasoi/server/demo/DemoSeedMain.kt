package app.rasoi.server.demo

import app.rasoi.core.contract.ContractJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking

/**
 * Seeds the demo household into a running dev server (`RASOI_AUTH_MODE=dev`) with an empty database:
 * `RASOI_DEMO_URL=http://localhost:8080 ./gradlew :server:app:seedDemo`. Sign in to the app as +91 98000 00001.
 */
fun main() = runBlocking {
    val baseUrl = System.getenv("RASOI_DEMO_URL") ?: "http://localhost:8080"
    HttpClient(OkHttp) { install(ContentNegotiation) { json(ContractJson) } }.use { client ->
        val demo = DemoScenario(DemoApi(client, baseUrl)).run()
        println(
            "Seeded '${demo.household.name}' (${demo.household.id}) with ${demo.household.members.size} members " +
                "and ${demo.bills.size} draft bills",
        )
    }
}
