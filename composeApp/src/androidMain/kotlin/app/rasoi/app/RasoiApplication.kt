package app.rasoi.app

import android.app.Application

class RasoiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
