package app.rasoi.core.testing

import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.KitchenRepository
import app.rasoi.core.domain.kitchen.UseFirst
import app.rasoi.core.domain.kitchen.UseFirstEntry
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Ranks the given [items] with the real rules; set [failure] or [suggestionFailure] to make a call fail. */
class FakeKitchenRepository(
    var items: List<PantryItem> = emptyList(),
    var dishes: List<DishSuggestion> = emptyList(),
    private val clock: Clock = FixedClock(),
) : KitchenRepository {
    var failure: AppError? = null
    var suggestionFailure: AppError? = null
    var suggestionCalls = 0

    override suspend fun useFirst(householdId: HouseholdId): AppResult<List<UseFirstEntry>> =
        failure?.let { AppResult.Failure(it) }
            ?: AppResult.Success(UseFirst.rank(items, clock.now().toLocalDateTime(TimeZone.UTC).date))

    override suspend fun suggestions(householdId: HouseholdId): AppResult<List<DishSuggestion>> {
        suggestionCalls++
        return (suggestionFailure ?: failure)?.let { AppResult.Failure(it) } ?: AppResult.Success(dishes)
    }
}
