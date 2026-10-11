package app.rasoi.app

/**
 * Phone sign-in implemented in Swift with the Firebase iOS SDK (ADR 0024). Results come back through the callback as
 * `(value, errorCode)`, with the codes documented on `CallbackPhoneAuthProvider`.
 */
interface NativePhoneAuth {
    fun requestCode(phone: String, callback: NativePhoneAuthCallback)

    fun confirm(verificationId: String, code: String, callback: NativePhoneAuthCallback)
}
