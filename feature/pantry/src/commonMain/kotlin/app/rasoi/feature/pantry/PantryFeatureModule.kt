package app.rasoi.feature.pantry

import app.rasoi.core.domain.pantry.AddPantryItemUseCase
import app.rasoi.core.domain.pantry.ListPantryUseCase
import app.rasoi.core.domain.pantry.UpdatePantryItemUseCase
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val pantryFeatureModule = module {
    factoryOf(::ListPantryUseCase)
    factoryOf(::AddPantryItemUseCase)
    factoryOf(::UpdatePantryItemUseCase)
    viewModel { (householdId: HouseholdId, role: Role) ->
        PantryViewModel(householdId, role, get(), get(), get(), get(), get())
    }
}
