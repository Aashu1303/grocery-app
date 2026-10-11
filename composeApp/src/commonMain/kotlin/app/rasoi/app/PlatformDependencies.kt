package app.rasoi.app

import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.session.SessionStorage
import app.rasoi.core.domain.auth.PhoneAuthProvider

/** What each platform entry point supplies: server address, secure session storage and phone sign-in. */
class PlatformDependencies(
    val apiConfig: ApiConfig,
    val sessionStorage: SessionStorage,
    val phoneAuth: PhoneAuthProvider,
)
