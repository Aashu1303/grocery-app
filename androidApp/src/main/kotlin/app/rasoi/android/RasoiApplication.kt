package app.rasoi.android

import android.app.Application
import app.rasoi.app.initKoin

class RasoiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
