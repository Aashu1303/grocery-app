package app.rasoi.server.support

import app.rasoi.server.notifications.Notifier
import app.rasoi.server.notifications.PushMessage

/** Remembers every push instead of sending it; tokens listed in [rejected] are reported as no longer valid. */
class RecordingNotifier : Notifier {
    data class Sent(val tokens: List<String>, val message: PushMessage)

    val sent = mutableListOf<Sent>()
    val rejected = mutableSetOf<String>()

    override suspend fun send(tokens: List<String>, message: PushMessage): List<String> {
        sent += Sent(tokens, message)
        return tokens.filter { it in rejected }
    }
}
