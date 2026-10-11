package app.rasoi.server.service

import app.rasoi.core.domain.kitchen.DishSuggestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Reads a chat model's `{"dishes":[…]}` answer; null when it is not that shape. Incomplete entries are skipped. */
internal object DishParser {
    fun parse(text: String): List<DishSuggestion>? = try {
        val dishes = Json.parseToJsonElement(text).jsonObject["dishes"] as? JsonArray
        dishes?.mapNotNull { dish(it.jsonObject) }
    } catch (_: IllegalArgumentException) { // malformed JSON or a value of the wrong type
        null
    }

    private fun dish(entry: JsonObject): DishSuggestion? {
        val name = entry["name"]?.jsonPrimitive?.contentOrNull
        val summary = entry["summary"]?.jsonPrimitive?.contentOrNull
        val uses = (entry["uses"] as? JsonArray)?.map { it.jsonPrimitive.content }
        return if (name == null ||
            summary == null ||
            uses == null
        ) {
            null
        } else {
            DishSuggestion(name.trim(), uses, summary.trim())
        }
    }
}
