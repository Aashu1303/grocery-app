package app.rasoi.feature.household

import app.rasoi.core.domain.auth.CompleteProfileUseCase
import app.rasoi.core.domain.auth.RequestOtpUseCase
import app.rasoi.core.domain.auth.VerifyOtpUseCase
import app.rasoi.core.domain.household.AddLocationUseCase
import app.rasoi.core.domain.household.CreateHouseholdUseCase
import app.rasoi.core.domain.household.CreateInviteUseCase
import app.rasoi.core.domain.household.JoinHouseholdUseCase
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.feature.household.detail.HouseholdViewModel
import app.rasoi.feature.household.setup.HouseholdSetupViewModel
import app.rasoi.feature.household.signin.SignInViewModel
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val householdFeatureModule = module {
    factoryOf(::RequestOtpUseCase)
    factoryOf(::VerifyOtpUseCase)
    factoryOf(::CompleteProfileUseCase)
    factoryOf(::CreateHouseholdUseCase)
    factoryOf(::JoinHouseholdUseCase)
    factoryOf(::CreateInviteUseCase)
    factoryOf(::AddLocationUseCase)
    viewModelOf(::SignInViewModel)
    viewModel { HouseholdSetupViewModel(get(), get(), timeZoneId = { TimeZone.currentSystemDefault().id }) }
    viewModel { (householdId: HouseholdId, role: Role) -> HouseholdViewModel(householdId, role, get(), get(), get()) }
}
