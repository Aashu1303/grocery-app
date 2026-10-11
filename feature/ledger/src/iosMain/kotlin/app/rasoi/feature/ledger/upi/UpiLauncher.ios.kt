package app.rasoi.feature.ledger.upi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberUpiLauncher(): UpiLauncher = remember {
    UpiLauncher { link ->
        val url = NSURL.URLWithString(link)
        if (url == null) {
            false
        } else {
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
            true
        }
    }
}
