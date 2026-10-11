package app.rasoi.server.demo

import app.rasoi.core.model.AppResult
import app.rasoi.server.parsing.fallback.ChatCompletionClient
import app.rasoi.server.support.TestScope
import app.rasoi.server.support.rasoiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Runs the end-to-end demo scenario and proves it exercises every endpoint the server registers. */
class DemoScenarioTest {
    /** A model that suggests one dish from the first two ingredients it is given. */
    private val chat = ChatCompletionClient { _, user ->
        val items = user.lines().filter {
            it.startsWith("- ")
        }.map { it.removePrefix("- ").substringBefore(" (use soon)") }
        val uses = items.take(2).joinToString(",") { "\"${it.replace("\"", "")}\"" }
        AppResult.Success("""{"dishes":[{"name":"Kitchen special","uses":[$uses],"summary":"Cook them together."}]}""")
    }

    private fun TestScope.hooks() = object : DemoHooks {
        override val hasChatModel = true

        override suspend fun approvalWindowPasses() {
            clock.advance(49.hours)
            graph.bills.autoApproveDue()
        }

        override suspend fun daysPass(days: Int): List<String> {
            clock.advance(days.days)
            graph.expiryAlerts.sendDue()
            return notifier.sent.map { it.message.title }
        }
    }

    @Test
    fun demoScenarioPassesAndCoversEveryEndpoint() = rasoiTest(chat = chat) {
        val api = DemoApi(client)
        DemoScenario(api, hooks()).run()
        assertEquals(application.registeredEndpoints(), api.exercised)
    }
}
