# 0024. Release builds, production sign-in and the server image

Status: Accepted
Date: 2026-10-11

## Context
ADR 0016 left the production `PhoneAuthProvider` implementations and release settings to M7. A release must not ship
the development provider, must talk HTTPS only, and takes its secrets from outside the repository. None of the Firebase
parts can be exercised without the owner's Firebase project, so what can be verified is that they compile and that the
unconfigured paths fail visibly.

## Decision
- **Android.** The sign-in provider is chosen by source set: `src/debug` binds `DevPhoneAuthProvider`, `src/release`
  binds `FirebasePhoneAuthProvider`, so the development provider is not in the release code at all. The Firebase Auth
  SDK is a `releaseImplementation` dependency. `FirebaseApp` is initialised in code from three Gradle properties
  (`rasoi.firebase.appId`, `.apiKey`, `.projectId`) instead of the `google-services` plugin and JSON file, so the build
  needs no secret file; without them a release answers sign-in with an `Unavailable` error. The reCAPTCHA fallback
  needs a foreground activity, tracked by `CurrentActivity`.
- **iOS.** Swift implements `NativePhoneAuth` (exported from the Kotlin framework) with the Firebase iOS SDK, added as
  a Swift package in `project.yml`. Kotlin adapts its callbacks with `CallbackPhoneAuthProvider` (`core:data`, unit
  tested). `#if DEBUG` passes no provider and Kotlin falls back to the development one; release always passes Firebase,
  or a stand-in that fails visibly when `GoogleService-Info.plist` is absent. The reCAPTCHA return scheme is the
  `RASOI_FIREBASE_REVERSED_CLIENT_ID` build setting.
- **HTTPS only.** `preReleaseBuild` (Android) and a pre-build script (iOS) refuse a release whose API address is not
  `https://`. Cleartext stays in the Android debug manifest.
- **Signing** comes from `RASOI_KEYSTORE_FILE`, `RASOI_KEYSTORE_PASSWORD`, `RASOI_KEY_ALIAS` and `RASOI_KEY_PASSWORD`
  for Android and from Xcode/`xcodebuild` settings for iOS; an unsigned release still builds, which is what CI does.
- **Shrinking stays off.** R8 on a Compose, Ktor, Koin and Firebase app needs keep rules that can only be checked on
  a device; shipping untested rules is worse than a larger APK.
- **Server image.** A multi-stage `Dockerfile` builds `:server:app:installDist` with every client module switched off
  and runs it as a non-root user with `/data` as the storage volume. All configuration is environment variables.
- **Not built:** the apps do not yet fetch an FCM token and call `POST /devices`. The server side (token store, FCM
  sender, sweeps) is complete and tested; the client side needs FCM and APNs setup in the same Firebase project and
  can only be verified on devices.

## Consequences
- CI compiles the release variant (Firebase code included) and the iOS app (Firebase package included) but cannot
  sign in; the first real check is an owner's build with their Firebase project, described in `docs/running-locally.md`.
- A release built without Firebase settings starts and shows an error at sign-in rather than crashing.
