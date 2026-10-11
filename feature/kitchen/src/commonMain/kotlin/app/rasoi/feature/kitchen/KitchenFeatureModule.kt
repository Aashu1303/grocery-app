package app.rasoi.feature.kitchen

import app.rasoi.core.domain.kitchen.GetUseFirstUseCase
import app.rasoi.core.domain.kitchen.SuggestDishesUseCase
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val kitchenFeatureModule = module {
    factoryOf(::GetUseFirstUseCase)
    factoryOf(::SuggestDishesUseCase)
    viewModel { (householdId: HouseholdId, role: Role) -> KitchenViewModel(householdId, role, get(), get()) }
}
