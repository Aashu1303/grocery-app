package app.rasoi.server.demo

/**
 * What only an in-process run can do: move the clock and run the server's sweeps. Against a live server there are no
 * hooks, so the steps that depend on them are skipped.
 */
interface DemoHooks {
    /** True when the server has a chat model, so dish suggestions can be checked. */
    val hasChatModel: Boolean

    /** 48 hours pass without an answer, and the auto-approve sweep runs. */
    suspend fun approvalWindowPasses()

    /** [days] pass and the expiry-alert sweep runs; returns the titles of every push sent so far. */
    suspend fun daysPass(days: Int): List<String>
}
