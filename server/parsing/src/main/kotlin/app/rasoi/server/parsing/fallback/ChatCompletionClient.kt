package app.rasoi.server.parsing.fallback

import app.rasoi.core.model.AppResult

/** One request to a chat model that answers with a JSON object as text. */
fun interface ChatCompletionClient {
    suspend fun completeJson(system: String, user: String): AppResult<String>
}
