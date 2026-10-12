---
name: kmp-project-structure
description: Decide where Kotlin Multiplatform code and modules belong in Rasoi (entry points, shared logic vs shared UI, server-shared core, source sets, expect/actual) and add or move a module the right way. Use when creating a module, moving code between modules, adding a platform target or entry point, or touching settings.gradle.kts or the convention plugins.
---

# KMP project structure

Rasoi follows JetBrains' recommended KMP layout
(https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html), adapted to its module
map in AGENTS.md and ADRs 0007, 0010, 0012 and 0013. `./gradlew verifyModuleGraph` enforces the dependency edges.

## 1. The layout and why

| Guide's role | Rasoi module(s) | Rule |
| --- | --- | --- |
| App entry points, one module each | `androidApp`, `iosApp` (Xcode, ADR 0012), `server:app` | Thin: only the platform entry (Activity, Application, manifest, `@main`, Ktor `main`) and build-type wiring. No screens, no business logic. |
| Shared code with the server (`core`) | `core:model`, `core:domain`, `core:contract` | Plain KMP (`rasoi.kmp-library`), no Compose, no Android or iOS APIs. |
| Shared logic for clients (`sharedLogic`) | `core:data` | Ktor client, SQLDelight, session storage. No Compose. |
| Shared UI (`sharedUI`) | `core:designsystem`, `feature:*`, `composeApp` | Compose Multiplatform. `composeApp` is the app's shared root: navigation, Koin wiring, the iOS framework. |
| Server-only code | `server:*` | JVM (`rasoi.jvm-server`), never depends on Compose or `core:data`. |

Logic and UI are separate modules so a native UI (for example SwiftUI) or the server can use the logic without
pulling in Compose. Keep it that way: never apply Compose to a `core` logic module.

## 2. Where does this code go?

1. Runs on the server too (entities, money, validation, DTOs)? `core:model` / `core:domain` / `core:contract`.
2. Talks to the network, disk or secure storage? `core:data`, behind an interface from `core:domain`.
3. A reusable visual component or token? `core:designsystem` (`add-design-component` skill).
4. A screen or ViewModel? Its `feature:*` module (`new-feature-module` skill). Code that two features need moves down
   into `core`, never sideways into another feature.
5. Navigation, DI graph, app-wide state? `composeApp`.
6. Needs `Activity`, `Application`, the manifest, signing or Firebase plugin config? `androidApp`.
7. Needs Swift or Xcode settings? `iosApp`.

## 3. Source sets and expect/actual

- Default to `commonMain`. Platform source sets (`androidMain`, `iosMain`, `jvmMain`) hold only `actual`s and
  platform glue.
- `expect`/`actual` declarations live in the shared module that owns the API (for example `PdfPicker` in
  `feature:invoiceimport`, `PlatformEngine` in `core:data`), never in `androidApp` or `iosApp`. Allowed only for file
  picking, sharing, notifications and secure storage, each behind an interface (AGENTS.md rule 7).
- Every KMP module has a `jvm()` target (ADR 0007): it runs fast tests and the Roborazzi screenshots. Give each
  `expect` a `jvmMain` actual, even a no-op. It is not a desktop app; a real desktop app would get its own
  `desktopApp` entry module.
- `composeApp` has no `androidMain`: Android-only app code goes in `androidApp/src/main` (and `src/debug`,
  `src/release` for build-type variants).

## 4. Android (AGP 9)

- Shared modules use AGP's KMP library plugin (`com.android.kotlin.multiplatform.library`) through
  `rasoi.android-library`, which the `rasoi.kmp-library` plugin applies when `rasoi.android=true`. Never apply
  `com.android.library` or `com.android.application` to a KMP module; AGP 9 rejects it.
- Namespace is derived from the module path (`app.rasoi.<path>`); Android resources are enabled for Compose resources.
- Only `androidApp` applies `rasoi.android-application` and owns `applicationId`, version, `targetSdk`, signing and
  the google-services plugin. Its sources are in `src/main`, not `src/androidMain`.

## 5. iOS

- `composeApp` exports the static framework with `baseName = "ComposeApp"` (`ComposeApplicationPlugin`). Swift imports
  the base name (`import ComposeApp`), not the module name; change both together.
- The Xcode build phase runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` (`iosApp/project.yml`); after
  changing it, run `cd iosApp && xcodegen generate`.
- If iOS ever switches to native UI, export a framework from a logic-only module instead of `composeApp`.

## 6. Adding a module

1. Pick the convention plugin: `rasoi.kmp-library` (logic), `rasoi.compose-library` (UI), `rasoi.feature` (feature),
   `rasoi.jvm-server` (server). Never configure Kotlin, Android or quality tooling by hand in the module.
2. `include(...)` it in `settings.gradle.kts`, inside the `composeEnabled` block if it uses Compose (ADR 0007), so
   machines without Google Maven still build.
3. Compose resources: set `packageOfResClass = "app.rasoi.<area>.resources"` and `publicResClass = false`.
4. Add or update a rule in `build-logic/.../ModuleRules.kt` if the new edges need one, and the module map in
   AGENTS.md.
5. A new kind of module or a new top-level entry point is an architecture decision: write an ADR (`write-adr` skill).
6. Run `./gradlew verifyModuleGraph check`, and `iosSimulatorArm64Test` if iOS code changed.

## Do not

- Put business logic or screens in `androidApp`, `iosApp` or `server:app`.
- Make a `core` logic module depend on Compose, or `core:domain` depend on anything but `core:model`.
- Make a feature depend on another feature or on `core:data`.
- Add JS/Wasm or desktop targets without an ADR and their own entry module (`webApp`, `desktopApp`).
