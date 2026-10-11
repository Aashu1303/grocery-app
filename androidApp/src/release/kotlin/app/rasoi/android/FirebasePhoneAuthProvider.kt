package app.rasoi.android

import app.rasoi.core.domain.auth.PhoneAuthProvider
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import com.google.firebase.auth.PhoneAuthProvider as FirebasePhone

/** Phone OTP with the Firebase Auth SDK; the ID token it returns is what the server verifies (ADR 0015). */
internal class FirebasePhoneAuthProvider(private val auth: FirebaseAuth, private val activities: CurrentActivity) :
    PhoneAuthProvider {
    override suspend fun requestCode(phone: PhoneNumber): AppResult<String> {
        val activity = activities.current ?: return AppResult.Failure(AppError.Unexpected("no_foreground_activity"))
        return suspendCancellableCoroutine { continuation ->
            val callbacks = object : FirebasePhone.OnVerificationStateChangedCallbacks() {
                // Instant verification still sends a code; the user types it, so there is nothing to do here.
                override fun onVerificationCompleted(credential: PhoneAuthCredential) = Unit

                override fun onVerificationFailed(error: FirebaseException) {
                    if (continuation.isActive) continuation.resume(AppResult.Failure(error.toAppError("phone")))
                }

                override fun onCodeSent(verificationId: String, token: FirebasePhone.ForceResendingToken) {
                    if (continuation.isActive) continuation.resume(AppResult.Success(verificationId))
                }
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone.e164)
                .setTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            FirebasePhone.verifyPhoneNumber(options)
        }
    }

    override suspend fun confirm(verificationId: String, code: String): AppResult<String> =
        suspendCancellableCoroutine { continuation ->
            fun finish(result: AppResult<String>) {
                if (continuation.isActive) continuation.resume(result)
            }
            auth.signInWithCredential(FirebasePhone.getCredential(verificationId, code))
                .addOnFailureListener { finish(AppResult.Failure(it.toAppError("code"))) }
                .addOnSuccessListener { signedIn ->
                    val user = signedIn.user
                    if (user == null) {
                        finish(AppResult.Failure(AppError.Unauthenticated))
                    } else {
                        user.getIdToken(false)
                            .addOnFailureListener { finish(AppResult.Failure(it.toAppError("code"))) }
                            .addOnSuccessListener { token ->
                                val idToken = token.token
                                finish(
                                    if (idToken == null) {
                                        AppResult.Failure(AppError.Unauthenticated)
                                    } else {
                                        AppResult.Success(idToken)
                                    },
                                )
                            }
                    }
                }
        }

    /** A rejected number or code is the user's to fix; anything else is reported without Firebase's wording. */
    private fun Exception.toAppError(field: String): AppError = when (this) {
        is FirebaseNetworkException -> AppError.Offline
        is FirebaseAuthInvalidCredentialsException ->
            AppError.Validation(listOf(AppError.FieldViolation(field, if (field == "code") "incorrect" else "invalid")))
        else -> AppError.Unexpected(this::class.java.simpleName)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
    }
}
