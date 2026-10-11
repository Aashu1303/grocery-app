import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        let apiBaseUrl = Bundle.main.object(forInfoDictionaryKey: "RasoiApiBaseUrl") as? String
        KoinIosKt.doInitKoinIos(apiBaseUrl: apiBaseUrl ?? "http://localhost:8080")
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
