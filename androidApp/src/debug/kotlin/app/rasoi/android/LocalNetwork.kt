package app.rasoi.android

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
private const val FIRST_SDK_WITH_LOCAL_NETWORK_PERMISSION = 37
private const val REQUEST_CODE = 1

/**
 * Debug builds reach the dev server at a private address (the emulator's 10.0.2.2), which Android 17 blocks until the
 * user grants the local network permission. Without this request a fresh install reports "You're offline".
 */
internal fun requestLocalNetworkAccess(activity: ComponentActivity) {
    if (Build.VERSION.SDK_INT < FIRST_SDK_WITH_LOCAL_NETWORK_PERMISSION) return
    if (activity.checkSelfPermission(ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED) return
    // The answer needs no handling: the next request simply succeeds or reports "You're offline" again.
    activity.requestPermissions(arrayOf(ACCESS_LOCAL_NETWORK), REQUEST_CODE)
}
