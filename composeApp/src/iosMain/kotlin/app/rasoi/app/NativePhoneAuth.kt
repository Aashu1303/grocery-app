package app.rasoi.app

/**
 * Phone sign-in implemented in Swift with the Firebase iOS SDK (ADR 0024). Results come back through [done] as
 * `(value, errorCode)`: a verification id or ID token on success, otherwise one of the codes documented on
 * `CallbackPhoneAuthProvider`.
 */
interface NativePhoneAuth {
    fun requestCode(phone: String, done: (String?, String?) -> Unit)

    fun confirm(verificationId: String, code: String, done: (String?, String?) -> Unit)
}
