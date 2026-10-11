package app.rasoi.core.data.repository

import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.data.network.suggestDishes
import app.rasoi.core.data.network.useFirst
import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.KitchenRepository
import app.rasoi.core.domain.kitchen.UseFirstEntry
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.map

class DefaultKitchenRepository(private val api: RasoiApi) : KitchenRepository {
    override suspend fun useFirst(householdId: HouseholdId): AppResult<List<UseFirstEntry>> =
        api.useFirst(householdId.value).map { entries ->
            entries.map {
                UseFirstEntry(it.item.toModel(householdId), it.daysLeft, Freshness.valueOf(it.freshness.name))
            }
        }

    override suspend fun suggestions(householdId: HouseholdId): AppResult<List<DishSuggestion>> =
        api.suggestDishes(householdId.value).map { result ->
            result.dishes.map { DishSuggestion(it.name, it.uses, it.summary) }
        }
}
