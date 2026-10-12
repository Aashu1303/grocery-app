---
name: new-feature-module
description: Create or extend a Rasoi feature module (feature/*) with an MVI screen, ViewModel, UiState/UiEvent, strings and tests. Use when adding a new screen or feature area.
---

# New feature module

Features live in `feature/<name>/` and depend only on `core:model`, `core:domain` and `core:designsystem`
(enforced by `verifyModuleGraph`). Package: `app.rasoi.feature.<name>`. Unsure whether code belongs in a feature, in
`core` or in an entry module? See the `kmp-project-structure` skill.

## 1. Register the module (new feature only)

1. Add `":feature:<name>"` to the Compose block of `settings.gradle.kts`.
2. Create `feature/<name>/build.gradle.kts`:
   ```kotlin
   plugins { id("rasoi.feature") }

   compose.resources {
       publicResClass = false
       packageOfResClass = "app.rasoi.feature.<name>.resources"
       generateResClass = always
   }
   ```
3. Add the dependency in `composeApp/build.gradle.kts` and the route to `composeApp/.../Route.kt`.

## 2. Domain first (test-first)

- Repository interface in `core/domain/.../<area>/<Thing>Repository.kt`, returning `AppResult<T>` or
  `Flow<AppResult<T>>`.
- Use case `…UseCase` with one `operator fun invoke`. Write its tests in `core/domain/src/commonTest` **before** the
  implementation; keep the 90% Kover gate.
- Fakes for the repository go in `core:testing` (`Fake<Thing>Repository`), not in the feature.

## 3. MVI screen

Files in `feature/<name>/src/commonMain/kotlin/app/rasoi/feature/<name>/`:

- `<Screen>UiState.kt`: immutable `data class` (or sealed interface) covering loading, empty, error and content.
- `<Screen>UiEvent.kt`: `sealed interface` of user intents.
- `<Screen>Effect.kt`: one-off effects (navigation, snackbar) when needed.
- `<Screen>ViewModel.kt`: extends `androidx.lifecycle.ViewModel`, exposes `val state: StateFlow<…UiState>`,
  `val effects: Flow<…Effect>` (from a `Channel`) and `fun onEvent(event: …UiEvent)`. Inject use cases and `Clock`.
- `<Screen>Screen.kt`: a stateful `…Route` composable that collects state and a **stateless** `…Screen(state,
  onEvent, modifier = Modifier)` that renders it with design-system components only.
- Strings in `feature/<name>/src/commonMain/composeResources/values/strings.xml`. Buttons are verbs.
- Koin (ADR 0026): annotate the ViewModel `@KoinViewModel` and its screen arguments `@InjectedParam`; add a
  `@Module @ComponentScan class <Name>ViewModels` in the feature, register the feature's use cases with
  `factory<UseCase>()` in `useCaseModule` (`composeApp/.../UseCaseModule.kt`) and list `<Name>ViewModels::class` in
  `initKoin` (`composeApp/.../Koin.kt`). The plugin only follows definitions visible from the entry point (KOIN-W003).

## 4. Tests

- `commonTest/.../<Screen>ViewModelTest.kt` with Turbine: initial loading, content, empty, error and retry, and one
  test per `UiEvent`. Use `runTest` and a fake `Clock`.
- Screenshot every screen state (light and dark) once the module has a desktop screenshot test.

## 5. Finish

Run `./gradlew check` (plus `verifyFeatureSources`), then the `pre-commit-review` skill.
