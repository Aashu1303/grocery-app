# iosApp

Xcode wrapper for the shared `composeApp` framework. The Xcode project is generated, not committed:

```sh
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

The pre-build phase runs `:composeApp:embedAndSignAppleFrameworkForXcode`, so Kotlin changes are picked up on build.

Debug builds sign in with the development provider. Release builds use Firebase phone sign-in and need
`GoogleService-Info.plist` in `iosApp/iosApp/` plus an https `RASOI_API_BASE_URL`; see `docs/running-locally.md` and ADR 0024.
