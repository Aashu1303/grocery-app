package app.rasoi.app

import app.rasoi.core.data.auth.DevPhoneAuthProvider
import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.session.InMemorySessionStorage
import app.rasoi.core.domain.auth.AuthRepository
import app.rasoi.core.domain.bills.BillRepository
import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.pantry.ListPantryUseCase
import app.rasoi.core.domain.pantry.PantryRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.time.Clock

class KoinModulesTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun allModulesResolveTheirDependencies() {
        val platform = PlatformDependencies(
            ApiConfig("http://localhost:8080"),
            InMemorySessionStorage(),
            DevPhoneAuthProvider(),
        )
        val koin = startKoin { modules(listOf(appModule, dataModule(platform)) + featureModules) }.koin
        assertSame(Clock.System, koin.get<Clock>())
        assertNotNull(koin.get<AuthRepository>())
        assertNotNull(koin.get<HouseholdRepository>())
        assertNotNull(koin.get<BillRepository>())
        assertNotNull(koin.get<PantryRepository>())
        assertNotNull(koin.get<ListPantryUseCase>())
        assertNotNull(koin.get<GetBillUseCase>())
    }
}
