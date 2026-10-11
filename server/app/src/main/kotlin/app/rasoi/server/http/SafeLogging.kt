package app.rasoi.server.http

/**
 * Describes [cause] for logs without its message: database and parser messages can embed user data
 * (a duplicate phone number, an invoice line), and logs must stay free of PII (design-plan 3, security 3).
 */
internal fun describeForLog(cause: Throwable): String = generateSequence(cause) { it.cause }
    .take(MAX_CAUSES)
    .joinToString("\ncaused by ") { error ->
        error::class.qualifiedName + error.stackTrace.take(MAX_FRAMES).joinToString("") { "\n    at $it" }
    }

private const val MAX_CAUSES = 5
private const val MAX_FRAMES = 12
