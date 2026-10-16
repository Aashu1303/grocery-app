package app.rasoi.app

import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.network.HttpClientFactory
import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.data.repository.DefaultAuthRepository
import app.rasoi.core.data.repository.DefaultBillRepository
import app.rasoi.core.data.repository.DefaultHouseholdRepository
import app.rasoi.core.data.repository.DefaultKitchenRepository
import app.rasoi.core.data.repository.DefaultLedgerRepository
import app.rasoi.core.data.repository.DefaultPantryRepository
import app.rasoi.core.data.session.SessionHolder
import app.rasoi.core.data.session.SessionStorage
import app.rasoi.core.domain.auth.AuthRepository
import app.rasoi.core.domain.auth.PhoneAuthProvider
import app.rasoi.core.domain.bills.BillRepository
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.kitchen.KitchenRepository
import app.rasoi.core.domain.ledger.LedgerRepository
import app.rasoi.core.domain.pantry.PantryRepository
import app.rasoi.feature.bills.BillsViewModels
import app.rasoi.feature.household.HouseholdViewModels
import app.rasoi.feature.invoiceimport.InvoiceImportViewModels
import app.rasoi.feature.kitchen.KitchenViewModels
import app.rasoi.feature.ledger.LedgerViewModels
import app.rasoi.feature.pantry.PantryViewModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.modules
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import kotlin.time.Clock

val appModule: Module = module {
    single<Clock> { Clock.System }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    viewModel<AppViewModel>()
}

/**
 * What the platform entry point supplied, as definitions the rest of the graph can depend on. A parameterless module
 * keeps the entry point statically analysable; [PlatformDependencies] is declared at start-up by [initKoin].
 */
val platformModule: Module = module {
    single<PhoneAuthProvider> { get<PlatformDependencies>().phoneAuth }
    single<SessionStorage> { get<PlatformDependencies>().sessionStorage }
    single<ApiConfig> { get<PlatformDependencies>().apiConfig }
    single<IncomingInvoices> { get<PlatformDependencies>().incomingInvoices }
}

/** Everything built on the platform pieces: HTTP client, API and repositories. */
val dataModule: Module = module {
    single<SessionHolder>()
    single { HttpClientFactory.create() }
    single {
        val sessions = get<SessionHolder>()
        RasoiApi(get(), get<ApiConfig>(), token = sessions::token, onUnauthenticated = sessions::clear)
    }
    single<DefaultAuthRepository>() bind AuthRepository::class
    single<DefaultHouseholdRepository>() bind HouseholdRepository::class
    single<DefaultBillRepository>() bind BillRepository::class
    single<DefaultPantryRepository>() bind PantryRepository::class
    single<DefaultLedgerRepository>() bind LedgerRepository::class
    single<DefaultKitchenRepository>() bind KitchenRepository::class
}

/** Starts dependency injection once per process; called from each platform entry point. */
fun initKoin(platform: PlatformDependencies) {
    val application = startKoin {
        modules(
            platformModule,
            appModule,
            dataModule,
            useCaseModule,
        )
        modules(
            BillsViewModels::class,
            HouseholdViewModels::class,
            InvoiceImportViewModels::class,
            KitchenViewModels::class,
            LedgerViewModels::class,
            PantryViewModels::class,
        )
    }
    application.koin.declare(platform)
}
