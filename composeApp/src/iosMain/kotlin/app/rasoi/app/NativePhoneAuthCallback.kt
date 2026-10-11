package app.rasoi.app

/**
 * How Swift reports a sign-in step: the verification id or ID token as [value], or an error code as [error]. A
 * callback object rather than a lambda keeps the Swift signature free of Kotlin's `Unit` type.
 */
fun interface NativePhoneAuthCallback {
    fun onResult(value: String?, error: String?)
}
