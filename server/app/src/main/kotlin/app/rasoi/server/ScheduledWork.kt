package app.rasoi.server

import app.rasoi.server.jobs.JobRunner
import app.rasoi.server.jobs.ScheduledJob
import kotlin.time.Duration.Companion.minutes

/**
 * Every periodic job the server runs (ADR 0021), by name: each derives what is due from stored state and the clock, so
 * running one late or twice is safe. They run on the in-process timer, or from `POST /internal/sweeps` when an
 * external scheduler drives them (ADR 0028).
 */
internal fun sweeps(graph: AppGraph): Map<String, ScheduledJob> = linkedMapOf(
    "auto-approve-bills" to ScheduledJob { graph.bills.autoApproveDue() },
    "close-periods" to ScheduledJob { graph.periods.closeDue() },
    "expiry-alerts" to ScheduledJob { graph.expiryAlerts.sendDue() },
)

internal fun scheduleJobs(graph: AppGraph, runner: JobRunner) {
    sweeps(graph).forEach { (name, job) -> runner.every(name, SWEEP, job) }
}

private val SWEEP = 15.minutes
