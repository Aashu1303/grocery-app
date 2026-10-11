package app.rasoi.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppResultTest {
    private val success: AppResult<Int> = AppResult.Success(2)
    private val failure: AppResult<Int> = AppResult.Failure(AppError.Offline)

    @Test
    fun mapTransformsOnlySuccess() {
        assertEquals(AppResult.Success(4), success.map { it * 2 })
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun flatMapChainsAndShortCircuits() {
        assertEquals(AppResult.Success("2"), success.flatMap { AppResult.Success(it.toString()) })
        val forbidden: AppResult<String> = success.flatMap { AppResult.Failure(AppError.Forbidden) }
        assertEquals(AppResult.Failure(AppError.Forbidden), forbidden)
        assertEquals<AppResult<*>>(failure, failure.flatMap { AppResult.Success(it.toString()) })
    }

    @Test
    fun foldReachesTheMatchingBranch() {
        assertEquals("ok 2", success.fold(onSuccess = { "ok $it" }, onFailure = { "err $it" }))
        assertEquals("err Offline", failure.fold(onSuccess = { "ok $it" }, onFailure = { "err $it" }))
    }

    @Test
    fun accessorsExposeValueOrError() {
        assertEquals(2, success.getOrNull())
        assertNull(failure.getOrNull())
        assertNull(success.errorOrNull())
        assertEquals(AppError.Offline, failure.errorOrNull())
    }

    @Test
    fun sideEffectsRunForTheMatchingBranchOnly() {
        val seen = mutableListOf<String>()
        success.onSuccess { seen += "s$it" }.onFailure { seen += "f" }
        failure.onSuccess { seen += "s$it" }.onFailure { seen += "f$it" }
        assertEquals(listOf("s2", "fOffline"), seen)
    }

    @Test
    fun mapErrorTransformsOnlyFailure() {
        assertEquals(success, success.mapError { AppError.Timeout })
        assertEquals(AppResult.Failure(AppError.Timeout), failure.mapError { AppError.Timeout })
    }
}
