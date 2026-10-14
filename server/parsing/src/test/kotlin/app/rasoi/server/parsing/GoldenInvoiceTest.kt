package app.rasoi.server.parsing

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import app.rasoi.server.parsing.extract.ExtractedDocumentCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every redacted fixture in fixtures/invoices parses to its expected JSON snapshot, with no reconciliation
 * warnings (design-plan section 8: "every fixture green").
 */
class GoldenInvoiceTest {
    private val root = File(requireNotNull(System.getProperty("rasoi.fixtures")) { "rasoi.fixtures not set" })
    private val json = Json { prettyPrint = true }

    private fun fixtures(): List<File> =
        root.walkTopDown().filter { it.name.endsWith(".redacted.txt") }.sorted().toList()

    @Test
    fun everyPlatformHasFixtures() {
        assertEquals(
            setOf("blinkit", "firstclub", "handpickd", "instamart", "zepto"),
            fixtures().map { it.parentFile.name }.toSet(),
        )
    }

    @Test
    fun everyFixtureMatchesItsSnapshot() {
        val pipeline = InvoicePipeline.default()
        val failures = fixtures().mapNotNull { fixture ->
            val expected = json.parseToJsonElement(
                File(fixture.path.replace(".redacted.txt", ".expected.json")).readText(),
            )
            val actual = when (val result = pipeline.parse(ExtractedDocumentCodec.decode(fixture.readText()))) {
                is AppResult.Success -> snapshot(result.value)
                is AppResult.Failure -> JsonPrimitive("parse failed: ${result.error}")
            }
            val shown = { element: JsonElement -> json.encodeToString(JsonElement.serializer(), element) }
            "${fixture.relativeTo(root)}\nexpected ${shown(expected)}\nactual   ${shown(actual)}"
                .takeIf { actual != expected }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n\n"))
    }

    @Test
    fun snapshotsReconcileToTheirTotals() {
        fixtures().forEach { fixture ->
            val snapshot = json.parseToJsonElement(
                File(fixture.path.replace(".redacted.txt", ".expected.json")).readText(),
            ) as JsonObject
            assertTrue((snapshot["warnings"] as JsonArray).isEmpty(), "${fixture.name} expects warnings")
        }
    }

    private fun snapshot(result: ParseResult): JsonObject {
        val invoice = result.invoice
        return buildJsonObject {
            put("platform", invoice.platform.name)
            put("invoiceNumber", invoice.invoiceNumber)
            put("orderId", invoice.orderId)
            put("invoiceDate", invoice.invoiceDate.toString())
            put(
                "lines",
                buildJsonArray {
                    invoice.lines.forEach { line ->
                        add(
                            buildJsonObject {
                                put("rawName", line.rawName)
                                put("units", line.units)
                                put("amount", line.amount.toDecimalString())
                                line.measure?.let { put("measure", it.describe()) }
                                line.mrp?.let { put("mrp", it.toDecimalString()) }
                                line.hsn?.let { put("hsn", it) }
                                line.upc?.let { put("upc", it) }
                            },
                        )
                    }
                },
            )
            put(
                "charges",
                buildJsonArray {
                    invoice.charges.forEach { charge ->
                        add(
                            buildJsonObject {
                                put("kind", charge.kind.name)
                                put("label", charge.label)
                                put("amount", charge.amount.toDecimalString())
                            },
                        )
                    }
                },
            )
            put("total", invoice.total.toDecimalString())
            put("warnings", buildJsonArray { result.warnings.forEach { add(JsonPrimitive(it.toString())) } })
        }
    }

    private fun Quantity.describe(): String = when (unit) {
        QuantityUnit.MILLIGRAM -> "${amount / QuantityUnit.MILLIGRAM.perWhole} g"
        QuantityUnit.MILLILITRE -> "$amount ml"
        QuantityUnit.COUNT -> "${amount / QuantityUnit.COUNT.perWhole} pcs"
    }
}
