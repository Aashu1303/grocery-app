package app.rasoi.android

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.rasoi.app.RasoiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // The Compose intro's first frame matches the system splash, so drop it at once instead of fading it over
            // the zoom (ADR 0027).
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        requestLocalNetworkAccess(this)
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        setContent { RasoiApp(showCatalog = debuggable) }
    }
}
