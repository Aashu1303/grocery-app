package app.rasoi.core.domain.kitchen

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId

interface KitchenRepository {
    /** Food in stock, soonest expiry first. */
    suspend fun useFirst(householdId: HouseholdId): AppResult<List<UseFirstEntry>>

    /** Dish ideas using only what is in stock; fails with `Unavailable` when the server has no chat model. */
    suspend fun suggestions(householdId: HouseholdId): AppResult<List<DishSuggestion>>
}
