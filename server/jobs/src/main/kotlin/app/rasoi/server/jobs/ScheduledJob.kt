package app.rasoi.server.jobs

/** One piece of periodic server work. A job derives what is due from stored state, so a missed run only delays it. */
fun interface ScheduledJob {
    suspend fun run()
}
