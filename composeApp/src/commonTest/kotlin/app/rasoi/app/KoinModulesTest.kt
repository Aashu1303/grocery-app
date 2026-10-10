package app.rasoi.app

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.time.Clock

class KoinModulesTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun appModuleResolvesTheSystemClock() {
        val koin = startKoin { modules(appModule) }.koin
        assertSame(Clock.System, koin.get<Clock>())
    }
}
