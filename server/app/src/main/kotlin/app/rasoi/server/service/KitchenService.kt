package app.rasoi.server.service

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.DishSuggestions
import app.rasoi.core.domain.kitchen.UseFirst
import app.rasoi.core.domain.kitchen.UseFirstEntry
import app.rasoi.core.domain.ledger.PeriodRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.server.parsing.fallback.ChatCompletionClient
import app.rasoi.server.persistence.PantryStore
import org.slf4j.LoggerFactory
import kotlin.time.Clock

/** What to cook first and what to cook with it. Cooks may use both: they run the kitchen (design-plan 3). */
class KitchenService(
    private val households: HouseholdService,
    private val pantry: PantryStore,
    private val chat: ChatCompletionClient?,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(KitchenService::class.java)

    suspend fun useFirst(userId: UserId, householdId: HouseholdId): AppResult<List<UseFirstEntry>> =
        households.authorize(userId, householdId, Permission.VIEW_PANTRY).map {
            UseFirst.rank(pantry.list(householdId, PantryStatus.IN_STOCK), today(householdId))
        }

    /** Dish ideas from items in stock; nothing in stock means no call to the model. */
    suspend fun suggestions(userId: UserId, householdId: HouseholdId): AppResult<List<DishSuggestion>> =
        households.authorize(userId, householdId, Permission.VIEW_PANTRY).flatMap {
            val stock = pantry.list(householdId, PantryStatus.IN_STOCK)
            val ranked = UseFirst.rank(stock, today(householdId))
            when {
                ranked.isEmpty() -> AppResult.Success(emptyList())
                chat == null -> AppResult.Failure(AppError.Unavailable("suggestions_unavailable"))
                else -> ask(chat, stock, householdId)
            }
        }

    private suspend fun ask(
        chat: ChatCompletionClient,
        stock: List<app.rasoi.core.model.PantryItem>,
        householdId: HouseholdId,
    ): AppResult<List<DishSuggestion>> {
        val prompt = DishSuggestions.prompt(stock, today(householdId))
        val answer = chat.completeJson(prompt.system, prompt.user)
        if (answer is AppResult.Failure) {
            log.warn("Dish suggestions failed: {}", answer.error::class.simpleName)
            return AppResult.Failure(AppError.Unavailable("suggestions_failed"))
        }
        val dishes = DishParser.parse((answer as AppResult.Success).value)
            ?: return AppResult.Failure(AppError.Unavailable("suggestions_failed"))
        return AppResult.Success(DishSuggestions.restrictToStock(dishes, stock))
    }

    private suspend fun today(householdId: HouseholdId) =
        PeriodRules.today(clock.now(), households.timeZone(householdId))
}
