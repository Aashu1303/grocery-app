package app.rasoi.app

import app.rasoi.core.data.auth.CallbackPhoneAuthProvider
import app.rasoi.core.data.auth.DevPhoneAuthProvider
import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.session.KeychainSessionStorage

/**
 * Called from Swift at launch with the server URL from Info.plist. Debug builds pass no [nativePhoneAuth] and use the
 * development provider; release builds always pass the Firebase one (ADR 0024).
 */
fun initKoinIos(apiBaseUrl: String, nativePhoneAuth: NativePhoneAuth?) {
    val phoneAuth = nativePhoneAuth?.let(::callbackProvider) ?: DevPhoneAuthProvider()
    initKoin(PlatformDependencies(ApiConfig(apiBaseUrl), KeychainSessionStorage(), phoneAuth))
}

private fun callbackProvider(native: NativePhoneAuth) = CallbackPhoneAuthProvider(
    requestCode = { phone, done -> native.requestCode(phone) { value, error -> done(value, error) } },
    confirmCode = { id, code, done -> native.confirm(id, code) { value, error -> done(value, error) } },
)
