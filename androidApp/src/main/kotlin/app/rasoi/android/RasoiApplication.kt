package app.rasoi.android

import android.app.Application
import app.rasoi.app.IncomingInvoices
import app.rasoi.app.PlatformDependencies
import app.rasoi.app.initKoin
import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.session.KeystoreSessionStorage

class RasoiApplication : Application() {
    /** Invoices shared from other apps; [MainActivity] fills it and the app's import screen empties it. */
    val incomingInvoices = IncomingInvoices()

    override fun onCreate() {
        super.onCreate()
        initKoin(
            PlatformDependencies(
                apiConfig = ApiConfig(BuildConfig.API_BASE_URL),
                sessionStorage = KeystoreSessionStorage(this),
                phoneAuth = phoneAuthProvider(this),
                incomingInvoices = incomingInvoices,
            ),
        )
    }
}
