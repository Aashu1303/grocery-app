package app.rasoi.server.notifications

/** What a device shows; [data] travels with it for the app to act on. */
data class PushMessage(val title: String, val body: String, val data: Map<String, String> = emptyMap())

/** Sends a push to device tokens. Returns the tokens the provider says are no longer valid, so they can be dropped. */
fun interface Notifier {
    suspend fun send(tokens: List<String>, message: PushMessage): List<String>
}

/** Used when no push provider is configured: nothing is sent and every token is kept. */
object NoopNotifier : Notifier {
    override suspend fun send(tokens: List<String>, message: PushMessage): List<String> = emptyList()
}
