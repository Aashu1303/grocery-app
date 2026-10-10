# 0007. JVM targets for local verification; mobile and Compose verified in CI

Status: Accepted
Date: 2026-10-10

## Context
The development sandbox's network policy blocks Google Maven (`dl.google.com`) and Kotlin/Native downloads, and
there is no Xcode. Compose Multiplatform 1.12 depends on `androidx.*` artifacts that exist only on Google Maven,
so Compose modules cannot compile there, not even for desktop. Maven Central is reachable, through a mirror when
it rate-limits. Most logic is pure Kotlin.

## Decision
- Every shared module declares a `jvm` target. Domain, data, contract, view-model and server tests run on the JVM.
- Platform switches live in `gradle.properties`: `rasoi.android` and `rasoi.compose` default to `true`; `rasoi.ios`
  defaults to on only on macOS. A restricted machine turns them off in `~/.gradle/gradle.properties`; Compose modules
  are then excluded from the build in `settings.gradle.kts`.
- Android conventions live in a separate included build, `build-logic-android`, included only when
  `rasoi.android=true`, so machines without Google Maven never resolve AGP.
- GitHub Actions builds everything: Compose modules, the design-system Roborazzi screenshot tests (on desktop JVM),
  Android assembly on ubuntu, and iOS on macOS.
- Logic that features need to test (formatting, freshness, ranking) lives in pure-Kotlin modules, not in Compose code.

## Consequences
Fast local feedback for all shared logic and the server. Compose, Android and iOS regressions surface in CI only.
Build tooling should be tried with the switches both on and off.
