import ComposeApp
import FirebaseCore
import SwiftUI

@main
struct iOSApp: App {
    init() {
        let apiBaseUrl = Bundle.main.object(forInfoDictionaryKey: "RasoiApiBaseUrl") as? String
        KoinIosKt.doInitKoinIos(apiBaseUrl: apiBaseUrl ?? "http://localhost:8080", nativePhoneAuth: Self.phoneAuth())
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }

    /// Debug builds sign in with the development provider (nil); release builds use Firebase phone sign-in.
    private static func phoneAuth() -> NativePhoneAuth? {
        #if DEBUG
        return nil
        #else
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            return UnconfiguredNativePhoneAuth()
        }
        FirebaseApp.configure()
        return FirebaseNativePhoneAuth()
        #endif
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
