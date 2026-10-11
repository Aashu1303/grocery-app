package app.rasoi.server.demo

import app.rasoi.server.support.rasoiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

/** Runs the end-to-end demo scenario and proves it exercises every endpoint the server registers. */
class DemoScenarioTest {
    @Test
    fun demoScenarioPassesAndCoversEveryEndpoint() = rasoiTest {
        val api = DemoApi(client)
        DemoScenario(api) {
            clock.advance(49.hours)
            graph.bills.autoApproveDue()
        }.run()
        assertEquals(application.registeredEndpoints(), api.exercised)
    }
}
