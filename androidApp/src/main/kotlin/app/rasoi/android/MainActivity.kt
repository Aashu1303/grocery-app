package app.rasoi.android

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import app.rasoi.app.RasoiApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        // A recreated activity has already handed over the intent that started it.
        if (savedInstanceState == null) importShared(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        importShared(intent)
    }

    /** PDFs shared with "Share" or "Open with" go to the import screen (it waits for sign-in if needed). */
    private fun importShared(intent: Intent) {
        val uris = intent.sharedPdfUris()
        if (uris.isEmpty()) return
        val incoming = (application as RasoiApplication).incomingInvoices
        lifecycleScope.launch {
            incoming.offer(withContext(Dispatchers.IO) { uris.mapNotNull { readSharedPdf(it) } })
        }
    }
}
