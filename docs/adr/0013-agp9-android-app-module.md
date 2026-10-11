# 0013. AGP 9, compileSdk 37 and a thin androidApp module

Status: Accepted
Date: 2026-10-10

## Context
Compose Multiplatform 1.12 pulls androidx Compose 1.12 artifacts that require Android Gradle Plugin 9.1+ and
compileSdk 37. AGP 9 requires Gradle 9 and no longer supports `com.android.library` or `com.android.application`
together with `org.jetbrains.kotlin.multiplatform` in one module. Shared modules must use AGP's Kotlin Multiplatform
library plugin, and the Android application must live in its own module.

## Decision
- Gradle 9.4.1, AGP 9.1.0, compileSdk 37, targetSdk 36, minSdk 26.
- Every KMP module with `rasoi.android=true` applies `com.android.kotlin.multiplatform.library` through
  `rasoi.android-library` (namespace `app.rasoi.<module path>`).
- `composeApp` stays the shared app module (navigation graph, DI wiring, iOS framework export) as in the plan.
- New module `androidApp` holds only the Android entry: `Application`, `MainActivity` and the manifest. It depends on
  `composeApp` and applies `rasoi.android-application`.
- CI runs `check` with Android off (JVM, Compose desktop, lint) and then `:androidApp:assembleDebug`, so failures are
  attributed to the right layer.

## Consequences
The module layout gains `androidApp/` next to `composeApp/` and `iosApp/`, the same shape as the current KMP project
template. detekt 1.23 triggers one Gradle 9 deprecation warning (`ReportingExtension.file`) until detekt 2.
