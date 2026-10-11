package app.rasoi.server.jobs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Runs [ScheduledJob]s on a fixed delay inside [scope] (ADR 0021). A failing run is reported to [onFailure] and the
 * job keeps its schedule; only cancelling [scope] stops it. The caller owns the scope, so tests drive it with
 * virtual time and the server ties it to the application lifecycle.
 */
class JobRunner(private val scope: CoroutineScope, private val onFailure: (name: String, error: Throwable) -> Unit) {
    fun every(name: String, interval: Duration, job: ScheduledJob): Job {
        require(interval.isPositive()) { "interval must be positive" }
        return scope.launch {
            while (isActive) {
                runOnce(name, job)
                delay(interval)
            }
        }
    }

    // A job must never take the server down, whatever it throws; cancellation still ends the loop.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runOnce(name: String, job: ScheduledJob) {
        try {
            job.run()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onFailure(name, error)
        }
    }
}
