import ComposeApp
import FirebaseAuth
import Foundation

/// Phone OTP with the Firebase iOS SDK, reporting `(value, errorCode)` the way `CallbackPhoneAuthProvider` expects.
final class FirebaseNativePhoneAuth: NSObject, NativePhoneAuth {
    func requestCode(phone: String, callback: NativePhoneAuthCallback) {
        PhoneAuthProvider.provider().verifyPhoneNumber(phone, uiDelegate: nil) { verificationId, error in
            callback.onResult(value: verificationId, error: error.map(Self.errorCode))
        }
    }

    func confirm(verificationId: String, code: String, callback: NativePhoneAuthCallback) {
        let credential = PhoneAuthProvider.provider()
            .credential(withVerificationID: verificationId, verificationCode: code)
        Auth.auth().signIn(with: credential) { result, error in
            if let error = error {
                callback.onResult(value: nil, error: Self.errorCode(error))
                return
            }
            guard let user = result?.user else {
                callback.onResult(value: nil, error: "no_user")
                return
            }
            user.getIDToken { token, error in
                callback.onResult(value: token, error: error.map(Self.errorCode))
            }
        }
    }

    /// A rejected number or code is the user's to fix; a lost connection is retryable; the rest is reported by number.
    private static func errorCode(_ error: Error) -> String {
        let code = AuthErrorCode(rawValue: (error as NSError).code)
        switch code {
        case .invalidPhoneNumber?, .missingPhoneNumber?, .invalidVerificationCode?, .invalidVerificationID?:
            return "invalid"
        case .networkError?:
            return "offline"
        default:
            return "auth_\((error as NSError).code)"
        }
    }
}

/// Stands in when a release was built without `GoogleService-Info.plist`, so sign-in fails visibly instead of crashing.
final class UnconfiguredNativePhoneAuth: NSObject, NativePhoneAuth {
    func requestCode(phone: String, callback: NativePhoneAuthCallback) {
        callback.onResult(value: nil, error: "firebase_not_configured")
    }

    func confirm(verificationId: String, code: String, callback: NativePhoneAuthCallback) {
        callback.onResult(value: nil, error: "firebase_not_configured")
    }
}
