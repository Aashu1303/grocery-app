package app.rasoi.app

import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.session.SessionStorage
import app.rasoi.core.domain.auth.PhoneAuthProvider
import org.koin.core.annotation.Provided

/**
 * What each platform entry point supplies: server address, secure session storage, phone sign-in, and the inbox for
 * invoices other apps share to Rasoi.
 */
@Provided
class PlatformDependencies(
    val apiConfig: ApiConfig,
    val sessionStorage: SessionStorage,
    val phoneAuth: PhoneAuthProvider,
    val incomingInvoices: IncomingInvoices = IncomingInvoices(),
)
