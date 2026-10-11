package app.rasoi.server.parsing.fallback

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.server.parsing.extract.ExtractedDocumentCodec
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LlmInvoiceFallbackTest {
    private val document = documentOf(
        "Fresh Mart Tax Invoice",
        "Customer Name: Asha Rao",
        "Invoice No: FM-1001 Order: 77001 Date: 04/10/2026",
        "Tomato Hybrid 1 40.00 40.00",
        "Paneer 1 90.00 90.00",
        "Delivery fee 20.00",
        "Total 150.00",
    )
    private val answer = """
        {"lines":[{"name":"Tomato Hybrid","units":1,"amount":"40.00"},{"name":"Paneer","amount":"90"}],
         "charges":[{"label":"Delivery fee","amount":"20.00"}],"total":"150.00","note":"ignored"}
    """.trimIndent()

    private class RecordingClient(private val reply: AppResult<String>) : ChatCompletionClient {
        val prompts = mutableListOf<String>()

        override suspend fun completeJson(system: String, user: String): AppResult<String> {
            prompts += user
            return reply
        }
    }

    private fun unreadable(what: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("document", "unreadable_$what"))))

    @Test
    fun buildsAnInvoiceFromTheValidatedAnswerAndLocalHeader() = runTest {
        val client = RecordingClient(AppResult.Success(answer))
        val result = LlmInvoiceFallback(client).parse(document)
        val expected = ParsedInvoice(
            platform = Platform.OTHER,
            invoiceNumber = "FM-1001",
            orderId = "77001",
            invoiceDate = LocalDate(2026, 10, 4),
            lines = listOf(ParsedLine("Tomato Hybrid", 1, Money(4_000)), ParsedLine("Paneer", 1, Money(9_000))),
            charges = listOf(ParsedCharge(ChargeKind.DELIVERY, "Delivery fee", Money(2_000))),
            total = Money(15_000),
        )
        assertEquals(AppResult.Success(expected), result)
    }

    @Test
    fun sendsOnlyTheItemTable() = runTest {
        val client = RecordingClient(AppResult.Success(answer))
        LlmInvoiceFallback(client).parse(document)
        val sent = client.prompts.single()
        assertTrue(sent.startsWith("Tomato Hybrid"))
        listOf("Asha", "FM-1001", "77001", "Invoice").forEach { assertFalse(it in sent, "leaked $it") }
    }

    @Test
    fun neverSendsHeaderPlaceholdersFromRealLayouts() = runTest {
        val root = File(requireNotNull(System.getProperty("rasoi.fixtures")))
        root.walkTopDown().filter { it.name.endsWith(".redacted.txt") }.forEach { fixture ->
            val client = RecordingClient(AppResult.Success(answer))
            LlmInvoiceFallback(client).parse(ExtractedDocumentCodec.decode(fixture.readText()))
            client.prompts.forEach { sent ->
                listOf("CUSTOMER", "SELLER_ADDRESS", "INV0", "Invoice No").forEach {
                    assertFalse(it in sent, "${fixture.name} leaked $it")
                }
            }
        }
    }

    @Test
    fun rejectsAnswersThatAreNotTheSchema() = runTest {
        listOf(
            "not json",
            """{"lines":[],"total":"0.00"}""",
            """{"lines":[{"name":"Tea","amount":"ten"}],"total":"10.00"}""",
            """{"lines":[{"name":" ","amount":"10.00"}],"total":"10.00"}""",
            """{"lines":[{"name":"Tea","units":0,"amount":"10.00"}],"total":"10.00"}""",
            """{"lines":[{"name":"Tea","amount":"10.00"}],"charges":[{"label":"Fee","amount":"x"}],"total":"10.00"}""",
            """{"lines":[{"name":"Tea","amount":"10.00"}]}""",
        ).forEach { reply ->
            assertEquals(
                unreadable("fallback"),
                LlmInvoiceFallback(RecordingClient(AppResult.Success(reply))).parse(document),
                reply,
            )
        }
    }

    @Test
    fun doesNotCallTheModelWithoutAnInvoiceNumberDateOrTable() = runTest {
        val client = RecordingClient(AppResult.Success(answer))
        val fallback = LlmInvoiceFallback(client)
        assertEquals(unreadable("invoice_number"), fallback.parse(documentOf("Date 04/10/2026", "Tea 1 10.00 10.00")))
        assertEquals(unreadable("date"), fallback.parse(documentOf("Invoice No: A1", "Tea 1 10.00 10.00")))
        assertEquals(unreadable("lines"), fallback.parse(documentOf("Invoice No: A1", "Date 04/10/2026", "Thanks")))
        assertEquals(emptyList(), client.prompts)
    }

    @Test
    fun passesClientFailuresThrough() = runTest {
        val result = LlmInvoiceFallback(RecordingClient(AppResult.Failure(AppError.Timeout))).parse(document)
        assertIs<AppError.Timeout>(assertIs<AppResult.Failure>(result).error)
    }
}
