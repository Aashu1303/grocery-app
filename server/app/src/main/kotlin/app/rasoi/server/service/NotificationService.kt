package app.rasoi.server.service

import app.rasoi.core.model.UserId
import app.rasoi.server.notifications.Notifier
import app.rasoi.server.notifications.PushMessage
import app.rasoi.server.persistence.DeviceStore
import org.slf4j.LoggerFactory

/**
 * Sends pushes to members' phones. A push is a courtesy, never part of a request's result: a provider outage is
 * logged by its type only (no tokens, no text) and never fails the caller, and tokens the provider rejects are dropped.
 */
class NotificationService(private val notifier: Notifier, private val devices: DeviceStore) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    // The provider is outside our control, so whatever it throws must not reach the user's request.
    @Suppress("TooGenericExceptionCaught")
    suspend fun notify(users: Collection<UserId>, message: PushMessage) {
        try {
            val tokens = devices.tokensFor(users)
            if (tokens.isNotEmpty()) devices.remove(notifier.send(tokens, message))
        } catch (error: Exception) {
            log.warn("Push failed: {}", error::class.simpleName)
        }
    }
}
