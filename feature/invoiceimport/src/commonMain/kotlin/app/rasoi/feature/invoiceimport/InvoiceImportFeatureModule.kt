package app.rasoi.feature.invoiceimport

import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.bills.ImportInvoicesUseCase
import app.rasoi.core.domain.bills.UpdateBillUseCase
import app.rasoi.core.model.BillId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.feature.invoiceimport.review.BillReviewViewModel
import app.rasoi.feature.invoiceimport.upload.ImportViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val invoiceImportFeatureModule = module {
    factoryOf(::ImportInvoicesUseCase)
    factoryOf(::GetBillUseCase)
    factoryOf(::UpdateBillUseCase)
    viewModel { (householdId: HouseholdId, role: Role) -> ImportViewModel(householdId, role, get()) }
    viewModel { (billId: BillId) -> BillReviewViewModel(billId, get(), get()) }
}
