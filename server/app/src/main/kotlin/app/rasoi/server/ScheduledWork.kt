package app.rasoi.server

import app.rasoi.server.jobs.JobRunner
import kotlin.time.Duration.Companion.minutes

/** Every periodic job the server runs (ADR 0021): each derives what is due from stored state and the clock. */
internal fun scheduleJobs(graph: AppGraph, runner: JobRunner) {
    runner.every("auto-approve-bills", SWEEP) { graph.bills.autoApproveDue() }
    runner.every("close-periods", SWEEP) { graph.periods.closeDue() }
}

private val SWEEP = 15.minutes
