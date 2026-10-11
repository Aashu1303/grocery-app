package app.rasoi.android

import android.app.Application
import app.rasoi.app.PlatformDependencies
import app.rasoi.app.initKoin
import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.session.KeystoreSessionStorage

class RasoiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            PlatformDependencies(
                apiConfig = ApiConfig(BuildConfig.API_BASE_URL),
                sessionStorage = KeystoreSessionStorage(this),
                phoneAuth = phoneAuthProvider(this),
            ),
        )
    }
}
