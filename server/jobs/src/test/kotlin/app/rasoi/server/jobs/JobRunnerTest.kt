package app.rasoi.server.jobs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class JobRunnerTest {
    @Test
    fun runsImmediatelyThenOnEveryInterval() = runTest {
        var runs = 0
        JobRunner(backgroundScope) { _, _ -> }.every("count", 10.minutes) { runs++ }
        runCurrent()
        assertEquals(1, runs)
        advanceTimeBy(10.minutes)
        runCurrent()
        assertEquals(2, runs)
        advanceTimeBy(20.minutes)
        runCurrent()
        assertEquals(4, runs)
    }

    @Test
    fun aFailingRunIsReportedAndTheScheduleContinues() = runTest {
        val failures = mutableListOf<String>()
        var runs = 0
        JobRunner(backgroundScope) { name, error -> failures += "$name:${error.message}" }.every("flaky", 1.minutes) {
            runs++
            check(runs != 1) { "boom" }
        }
        runCurrent()
        advanceTimeBy(1.minutes)
        runCurrent()
        assertEquals(listOf("flaky:boom"), failures)
        assertEquals(2, runs)
    }

    @Test
    fun cancellingTheScopeStopsTheJob() = runTest {
        var runs = 0
        val job = JobRunner(backgroundScope) { _, _ -> }.every("count", 1.minutes) { runs++ }
        runCurrent()
        job.cancel()
        advanceTimeBy(5.minutes)
        runCurrent()
        assertEquals(1, runs)
    }

    @Test
    fun rejectsAnIntervalThatIsNotPositive() = runTest {
        assertFailsWith<IllegalArgumentException> {
            JobRunner(backgroundScope) { _, _ -> }.every("bad", Duration.ZERO) {}
        }
    }
}
