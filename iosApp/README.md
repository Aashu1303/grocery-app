# iosApp

Xcode wrapper for the shared `composeApp` framework. The Xcode project is generated, not committed:

```sh
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

The pre-build phase runs `:composeApp:embedAndSignAppleFrameworkForXcode`, so Kotlin changes are picked up on build.
