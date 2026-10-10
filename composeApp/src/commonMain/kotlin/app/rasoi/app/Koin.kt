package app.rasoi.app

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

val appModule: Module = module {
    single<Clock> { Clock.System }
}

/** Starts dependency injection once per process; called from each platform entry point. */
fun initKoin() {
    startKoin { modules(appModule) }
}
