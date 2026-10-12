# 0026. Koin compiler plugin and annotations for client DI

Status: Accepted
Date: 2026-10-11

## Context

Hilt and Dagger are JVM/Android only; the client is Kotlin Multiplatform with an iOS target and keeps its
ViewModels in `commonMain`, so they cannot be used (ADR 0013). We still wanted compile-time checking of the
dependency graph and less hand-written wiring than `viewModel { (a, b) -> X(a, b, get(), get()) }`.

## Decision

- Stay on Koin and add the **Koin compiler plugin** (`io.insert-koin.compiler.plugin`) with `koin-annotations`.
  It is a Kotlin compiler plugin, so there is no KSP and no per-target setup.
- ViewModels are annotated `@KoinViewModel`; screen arguments (ids, roles) are `@InjectedParam`. Each feature has a
  `@Module @ComponentScan` class (`<Feature>ViewModels`) that collects them; `initKoin` loads it by class.
- Use cases and repositories use the plugin DSL (`factory<T>()`, `single<T>()`), so `core:domain` and `core:data`
  stay free of DI annotations. They are declared in `composeApp` (`useCaseModule`, `dataModule`, `platformModule`):
  the plugin cannot follow `module {}` values or `includes()` across Gradle modules, and reports KOIN-W003 and skips
  the full-graph check. Platform pieces reach the graph through a `@Provided` `PlatformDependencies` declared at start-up.
- The plugin is applied by `FeaturePlugin` and `ComposeApplicationPlugin` through `applyKoinCompiler()`.

## Consequences

- A missing dependency in a ViewModel or DSL definition fails the build (`KOIN-D001`), not the first screen open.
- The full graph is not verified at the entry point: Koin only checks the whole graph when every module is an
  annotated `@Module` class, and our use cases live in modules that must not depend on Koin. The plugin reports this as
  `KOIN-W003`, which it cannot silence, so `composeApp` no longer fails on warnings. `KoinModulesTest` still resolves
  the graph at runtime.
- The plugin has not yet verified Kotlin 2.4.21 (newest verified 2.4.20); the version warning is lowered to info.
  Revisit when a plugin release lists 2.4.21.
