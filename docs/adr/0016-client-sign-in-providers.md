# 0016. Client phone sign-in providers

Status: Accepted (production providers: ADR 0024)
Date: 2026-10-11

## Context
The app signs in with Firebase phone OTP (ADR 0004). The Firebase Android and iOS SDKs need a Firebase project
(`google-services.json`, `GoogleService-Info.plist`), APNs and reCAPTCHA configuration, which belong to the owner and
cannot be created or tested in this repository's CI. Development, CI and the demo scenario must still sign in.

## Decision
- `core:domain` defines `PhoneAuthProvider` (request a code, confirm it for an ID token). The sign-in flow, server
  session exchange and secure session storage are complete and tested against this interface.
- `DevPhoneAuthProvider` (`core:data`) sends no SMS, accepts code `123456` and returns `dev:<phone>` tokens, which only
  a server in `RASOI_AUTH_MODE=dev` accepts (ADR 0015). Debug builds bind it today.
- Production providers are added in M7 (hardening and release builds): Android via the Firebase Auth SDK, and iOS via
  a Swift implementation of the exported `PhoneAuthProvider` protocol using the Firebase iOS SDK, with configuration
  supplied outside the repository. Release builds must not ship the dev provider.
- Base URLs: Android `BuildConfig.API_BASE_URL` from the `rasoi.apiBaseUrl` Gradle property (default
  `http://10.0.2.2:8080`); iOS `RasoiApiBaseUrl` in Info.plist from the `RASOI_API_BASE_URL` build setting. Cleartext
  HTTP is allowed only in Android debug builds and for iOS local networking.

## Consequences
M1 delivers the full sign-in UX and server contract without a Firebase project. The M7 milestone carries the
production provider work and needs the owner's Firebase configuration.
