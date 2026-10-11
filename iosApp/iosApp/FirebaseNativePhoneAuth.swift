import ComposeApp
import FirebaseAuth
import Foundation

/// Phone OTP with the Firebase iOS SDK, reporting `(value, errorCode)` the way `CallbackPhoneAuthProvider` expects.
final class FirebaseNativePhoneAuth: NSObject, NativePhoneAuth {
    func requestCode(phone: String, done: @escaping (String?, String?) -> KotlinUnit) {
        PhoneAuthProvider.provider().verifyPhoneNumber(phone, uiDelegate: nil) { verificationId, error in
            _ = done(verificationId, error.map(Self.errorCode))
        }
    }

    func confirm(verificationId: String, code: String, done: @escaping (String?, String?) -> KotlinUnit) {
        let credential = PhoneAuthProvider.provider()
            .credential(withVerificationID: verificationId, verificationCode: code)
        Auth.auth().signIn(with: credential) { result, error in
            if let error = error {
                _ = done(nil, Self.errorCode(error))
                return
            }
            guard let user = result?.user else {
                _ = done(nil, "no_user")
                return
            }
            user.getIDToken { token, error in
                _ = done(token, error.map(Self.errorCode))
            }
        }
    }

    /// A rejected number or code is the user's to fix; a lost connection is retryable; the rest is reported by number.
    private static func errorCode(_ error: Error) -> String {
        let code = AuthErrorCode.Code(rawValue: (error as NSError).code)
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
    func requestCode(phone: String, done: @escaping (String?, String?) -> KotlinUnit) {
        _ = done(nil, "firebase_not_configured")
    }

    func confirm(verificationId: String, code: String, done: @escaping (String?, String?) -> KotlinUnit) {
        _ = done(nil, "firebase_not_configured")
    }
}
