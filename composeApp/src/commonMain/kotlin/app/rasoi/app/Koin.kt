package app.rasoi.app

import app.rasoi.core.data.network.HttpClientFactory
import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.data.repository.DefaultAuthRepository
import app.rasoi.core.data.repository.DefaultBillRepository
import app.rasoi.core.data.repository.DefaultHouseholdRepository
import app.rasoi.core.data.repository.DefaultLedgerRepository
import app.rasoi.core.data.repository.DefaultPantryRepository
import app.rasoi.core.data.session.SessionHolder
import app.rasoi.core.domain.auth.AuthRepository
import app.rasoi.core.domain.bills.BillRepository
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.ledger.LedgerRepository
import app.rasoi.core.domain.pantry.PantryRepository
import app.rasoi.feature.bills.billsFeatureModule
import app.rasoi.feature.household.householdFeatureModule
import app.rasoi.feature.invoiceimport.invoiceImportFeatureModule
import app.rasoi.feature.ledger.ledgerFeatureModule
import app.rasoi.feature.pantry.pantryFeatureModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

val appModule: Module = module {
    single<Clock> { Clock.System }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    viewModelOf(::AppViewModel)
}

/** One Koin module per feature module; the app and its tests load them all. */
val featureModules: List<Module> = listOf(
    householdFeatureModule,
    invoiceImportFeatureModule,
    pantryFeatureModule,
    billsFeatureModule,
    ledgerFeatureModule,
)

fun dataModule(platform: PlatformDependencies): Module = module {
    single { platform.phoneAuth }
    single { SessionHolder(platform.sessionStorage, get()) }
    single { HttpClientFactory.create() }
    single {
        val sessions = get<SessionHolder>()
        RasoiApi(get(), platform.apiConfig, token = sessions::token, onUnauthenticated = sessions::clear)
    }
    single<AuthRepository> { DefaultAuthRepository(get(), get(), get()) }
    single<HouseholdRepository> { DefaultHouseholdRepository(get()) }
    single<BillRepository> { DefaultBillRepository(get()) }
    single<PantryRepository> { DefaultPantryRepository(get()) }
    single<LedgerRepository> { DefaultLedgerRepository(get()) }
}

/** Starts dependency injection once per process; called from each platform entry point. */
fun initKoin(platform: PlatformDependencies) {
    startKoin { modules(listOf(appModule, dataModule(platform)) + featureModules) }
}
