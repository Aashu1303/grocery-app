# 0007. JVM and desktop targets for local verification; mobile verified in CI

Status: Accepted
Date: 2026-10-10

## Context
The development sandbox cannot download the Android SDK or Kotlin/Native and has no Xcode, so Android and iOS targets cannot be compiled there. Most logic is pure Kotlin.

## Decision
Every shared module also declares a `jvm` target. Domain, data, view-model and server tests run on the JVM. The design-system catalog and Roborazzi screenshot tests run on Compose Desktop. Android and iOS are compiled and tested in GitHub Actions (ubuntu and macOS).

## Consequences
Fast local feedback and full coverage of shared logic. Android and iOS regressions surface in CI, not locally. `expect`/`actual` code stays minimal and sits behind interfaces.
