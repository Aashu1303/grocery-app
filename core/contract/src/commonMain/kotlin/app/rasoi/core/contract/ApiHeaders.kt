package app.rasoi.core.contract

/** Header names shared by the app and the server. */
object ApiHeaders {
    /** Required on every mutating request so retries never apply a write twice (design-plan 3, correctness 7). */
    const val IDEMPOTENCY_KEY = "Idempotency-Key"
}
