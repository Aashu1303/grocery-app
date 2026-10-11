package app.rasoi.app

import app.rasoi.core.data.auth.DevPhoneAuthProvider
import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.session.KeychainSessionStorage

/** Called from Swift at launch with the server URL from Info.plist. */
fun initKoinIos(apiBaseUrl: String) {
    initKoin(PlatformDependencies(ApiConfig(apiBaseUrl), KeychainSessionStorage(), DevPhoneAuthProvider()))
}
