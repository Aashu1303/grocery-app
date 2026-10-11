package app.rasoi.server.parsing.fallback

import kotlinx.serialization.Serializable

/** The JSON object the model must answer with; amounts are decimal rupee strings. */
@Serializable
internal data class LlmInvoiceJson(val lines: List<Line>, val charges: List<Charge> = emptyList(), val total: String) {
    @Serializable
    data class Line(val name: String, val units: Int = 1, val amount: String)

    @Serializable
    data class Charge(val label: String, val amount: String)
}
