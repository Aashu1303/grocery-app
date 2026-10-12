package app.rasoi.app

import app.rasoi.core.domain.auth.CompleteProfileUseCase
import app.rasoi.core.domain.auth.RequestOtpUseCase
import app.rasoi.core.domain.auth.VerifyOtpUseCase
import app.rasoi.core.domain.bills.CreateManualBillUseCase
import app.rasoi.core.domain.bills.DecideBillUseCase
import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.bills.ImportInvoicesUseCase
import app.rasoi.core.domain.bills.ListBillsUseCase
import app.rasoi.core.domain.bills.SubmitBillUseCase
import app.rasoi.core.domain.bills.UpdateBillUseCase
import app.rasoi.core.domain.household.AddLocationUseCase
import app.rasoi.core.domain.household.CreateHouseholdUseCase
import app.rasoi.core.domain.household.CreateInviteUseCase
import app.rasoi.core.domain.household.JoinHouseholdUseCase
import app.rasoi.core.domain.kitchen.GetUseFirstUseCase
import app.rasoi.core.domain.kitchen.SuggestDishesUseCase
import app.rasoi.core.domain.ledger.CloseMonthUseCase
import app.rasoi.core.domain.ledger.GetLedgerUseCase
import app.rasoi.core.domain.ledger.MarkPaidUseCase
import app.rasoi.core.domain.ledger.UpdateUpiUseCase
import app.rasoi.core.domain.pantry.AddPantryItemUseCase
import app.rasoi.core.domain.pantry.ListPantryUseCase
import app.rasoi.core.domain.pantry.UpdatePantryItemUseCase
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory

/**
 * Every use case the feature screens depend on. Declared here, in the module that starts Koin, because the compiler
 * plugin only follows definitions it can see from the entry point (ADR 0026).
 */
val useCaseModule: Module = module {
    factory<CompleteProfileUseCase>()
    factory<RequestOtpUseCase>()
    factory<VerifyOtpUseCase>()
    factory<CreateManualBillUseCase>()
    factory<DecideBillUseCase>()
    factory<GetBillUseCase>()
    factory<ImportInvoicesUseCase>()
    factory<ListBillsUseCase>()
    factory<SubmitBillUseCase>()
    factory<UpdateBillUseCase>()
    factory<AddLocationUseCase>()
    factory<CreateHouseholdUseCase>()
    factory<CreateInviteUseCase>()
    factory<JoinHouseholdUseCase>()
    factory<GetUseFirstUseCase>()
    factory<SuggestDishesUseCase>()
    factory<CloseMonthUseCase>()
    factory<GetLedgerUseCase>()
    factory<MarkPaidUseCase>()
    factory<UpdateUpiUseCase>()
    factory<AddPantryItemUseCase>()
    factory<ListPantryUseCase>()
    factory<UpdatePantryItemUseCase>()
}
