package app.rasoi.feature.ledger

import app.rasoi.core.domain.ledger.CloseMonthUseCase
import app.rasoi.core.domain.ledger.GetLedgerUseCase
import app.rasoi.core.domain.ledger.MarkPaidUseCase
import app.rasoi.core.domain.ledger.UpdateUpiUseCase
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ledgerFeatureModule = module {
    factoryOf(::GetLedgerUseCase)
    factoryOf(::CloseMonthUseCase)
    factoryOf(::MarkPaidUseCase)
    factoryOf(::UpdateUpiUseCase)
    viewModel { (householdId: HouseholdId, me: UserId, role: Role) ->
        LedgerViewModel(householdId, me, role, get(), get(), get(), get(), get(), get())
    }
}
