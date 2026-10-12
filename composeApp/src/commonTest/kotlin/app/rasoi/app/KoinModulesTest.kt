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
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.feature.bills.list.BillsViewModel
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform
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
        initKoin(platform)
        val koin = KoinPlatform.getKoin()
        assertSame(Clock.System, koin.get<Clock>())
        assertNotNull(koin.get<AuthRepository>())
        assertNotNull(koin.get<HouseholdRepository>())
        assertNotNull(koin.get<BillRepository>())
        assertNotNull(koin.get<PantryRepository>())
        assertNotNull(koin.get<ListPantryUseCase>())
        assertNotNull(koin.get<GetBillUseCase>())
        assertNotNull(koin.get<BillsViewModel> { parametersOf(HouseholdId("h1"), Role.MEMBER) })
    }
}
