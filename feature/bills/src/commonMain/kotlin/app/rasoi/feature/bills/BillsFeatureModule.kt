package app.rasoi.feature.bills

import app.rasoi.core.domain.bills.CreateManualBillUseCase
import app.rasoi.core.domain.bills.DecideBillUseCase
import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.bills.ListBillsUseCase
import app.rasoi.core.domain.bills.SubmitBillUseCase
import app.rasoi.core.domain.bills.UpdateBillUseCase
import app.rasoi.core.model.BillId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import app.rasoi.feature.bills.detail.BillDetailViewModel
import app.rasoi.feature.bills.list.BillsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val billsFeatureModule = module {
    factoryOf(::ListBillsUseCase)
    factoryOf(::CreateManualBillUseCase)
    factoryOf(::GetBillUseCase)
    factoryOf(::UpdateBillUseCase)
    factoryOf(::SubmitBillUseCase)
    factoryOf(::DecideBillUseCase)
    viewModel { (householdId: HouseholdId, role: Role) -> BillsViewModel(householdId, role, get(), get(), get()) }
    viewModel { (billId: BillId, me: UserId, role: Role) ->
        BillDetailViewModel(billId, me, role, get(), get(), get(), get(), get())
    }
}
