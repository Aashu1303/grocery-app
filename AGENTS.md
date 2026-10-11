# AGENTS.md: Rasoi development guide

Rasoi is a shared-flat grocery and kitchen app: PDF invoice import, a live pantry, expiry tracking, dish suggestions
and fair bill splitting with month-end settlement. Kotlin everywhere: Compose Multiplatform (Android, iOS) and a Ktor
server that share `core:model`, `core:domain` and `core:contract`.

**Source of truth:** `docs/design-plan.md`. If you must deviate, update the plan and add an ADR under `docs/adr/`
(use the `write-adr` skill). Decisions so far are indexed in `docs/adr/README.md`.

## Module map

| Module | Kind | Owns | May depend on |
| --- | --- | --- | --- |
| `core:model` | KMP | Entities, `Money` (paise), `Quantity`, typed ids, `AppResult`/`AppError` | nothing internal |
| `core:domain` | KMP | Use cases and pure algorithms: split, settlement, expiry, categorizer; repository interfaces | `core:model` |
| `core:data` | KMP | Repository implementations, Ktor client, SQLDelight cache | `core:domain`, `core:contract` |
| `core:contract` | KMP | `@Serializable` DTOs (`…Dto`) and `ContractJson`, shared with the server | nothing internal |
| `core:designsystem` | KMP + Compose | Tokens, `AppTheme`, components, icons, catalog | `core:model` |
| `core:testing` | KMP | Fakes, builders, fixture loaders (test source sets only) | `core:model`, `core:domain` |
| `feature:*` | KMP + Compose | Screens, `…ViewModel`, `…UiState`, `…UiEvent` for one feature | `core:model`, `core:domain`, `core:designsystem` |
| `composeApp` | KMP + Compose | Shared app: navigation graph, Koin wiring, iOS framework | everything client-side |
| `androidApp` | Android app | `Application`, `MainActivity`, manifest (ADR 0013) | `composeApp` |
| `server:app` | JVM | Ktor entry, routing, auth, DI | `core:*` except `designsystem`, `data`; `server:*` |
| `server:parsing` | JVM | PDF extraction, platform detection, template parsers, LLM fallback | `core:model`, `core:domain`, `core:contract` |
| `server:jobs` | JVM | Auto-approve, expiry alerts, month close | as above |
| `server:notifications` | JVM | FCM | as above |
| `build-logic`, `build-logic-android` | Gradle | Convention plugins (`rasoi.*`) and rule checks | — |

`./gradlew verifyModuleGraph` enforces these edges (rules in `build-logic/.../ModuleRules.kt`).

## Build and test commands

```sh
./gradlew check                       # everything: ktlint, detekt, tests, Kover gate, module and design-system rules
./gradlew ktlintFormat                # auto-fix formatting
./gradlew :core:model:jvmTest         # one module's tests
./gradlew :server:app:test            # server tests
./gradlew :server:app:run             # run the server locally (PORT, default 8080)
./gradlew :androidApp:assembleDebug   # Android debug APK
./gradlew iosSimulatorArm64Test       # shared tests on the iOS simulator (macOS)
./gradlew recordRoborazziJvm          # re-record design-system screenshot goldens
./gradlew verifyRoborazziJvm          # compare screenshots against goldens
./scripts/check-no-real-invoices.sh   # privacy guard: no real invoices tracked
```

iOS app: `cd iosApp && xcodegen generate && open iosApp.xcodeproj` (ADR 0012).

### Platform switches (ADR 0007)

`gradle.properties` turns on `rasoi.android` and `rasoi.compose`; `rasoi.ios` defaults to on only on macOS. On a
machine without Google Maven or Kotlin/Native access, set them to `false` in `~/.gradle/gradle.properties`. Compose
modules are then excluded from the build, and CI verifies them instead. You can still lint them with
`./gradlew -Prasoi.compose=true :core:designsystem:ktlintCheck :core:designsystem:detekt`.

## Architecture rules

1. **Layers:** Compose UI → `ViewModel` → use cases → repositories. Dependencies point inward; `core:domain` imports
   nothing from data, UI or any platform.
2. **MVI:** each screen exposes one immutable `…UiState` as a `StateFlow`, accepts a sealed `…UiEvent` through one
   `onEvent` function, and sends one-off effects (navigation, snackbars) through a separate `Channel`-backed `Flow`.
3. **Errors are values:** repositories and use cases return `AppResult<T>` with a typed `AppError`. Never let an
   exception cross a layer boundary; catch at the edge (HTTP, DB, file) and map.
4. **Money** is `Money` (Long paise); **quantities** are `Quantity` (Long base units). Never `Float`/`Double`. Every
   split uses largest-remainder rounding and sums exactly to the source amount.
5. **Time:** store `kotlin.time.Instant` in UTC; convert to the household time zone only for display and month
   boundaries. Inject `kotlin.time.Clock`; never call `Clock.System` outside DI wiring.
6. **Server is authoritative** for bills, approvals and the ledger. Every mutating request carries an idempotency key.
   Every endpoint checks household membership and role before any read or write.
7. **`expect`/`actual`** only for file picking, sharing, notifications and secure storage, each behind an interface.
8. **Features never depend on other features.** Shared code moves down into `core`.
9. **Privacy:** no PII in logs. Real invoices never enter git (ADR 0009). Only line-item text goes to the LLM.

## Code style

- Official Kotlin coding conventions plus the Android Kotlin style guide for Compose; enforced by ktlint
  (`.editorconfig`, `intellij_idea` style, 120 columns) and detekt (`config/detekt/detekt.yml`, zero issues).
- Naming: `PascalCase` types and composables, `camelCase` functions and properties, `SCREAMING_SNAKE_CASE` constants,
  packages `app.rasoi.<area>` (for example `app.rasoi.feature.bills`).
- Suffixes: `…Screen`, `…ViewModel`, `…UiState`, `…UiEvent`, `…Repository`, `…UseCase`, `…Dto`, `…Entity`.
- Composables are stateless by default with state hoisted; `modifier: Modifier = Modifier` is the first optional
  parameter; every screen state has a preview and a screenshot.
- Features use **only** design-system tokens and components (`AppText`, `AppTextField`, `AppButton`, …): no raw
  `Color(...)`, no `dp`/`sp` literals, no direct `androidx.compose.material3` imports, no hard-coded strings
  (`./gradlew verifyFeatureSources`). Foundation layouts (`Column`, `Row`, `LazyColumn`, `Spacer`) are fine.
- Strings live in `composeResources/values/strings.xml` of the owning module.
- No `!!`, no `GlobalScope`, no TODO/FIXME comments, no commented-out code.
- One public class per file; small files and functions. Comments explain *why*; KDoc only on public domain APIs
  and non-obvious algorithms.
- No new dependency without a one-line justification in `libs.versions.toml` and the commit message.

## Testing

- `kotlin.test` everywhere; Turbine for flows; `kotlinx-coroutines-test` with virtual time.
- **Test-first** for `core:domain` and `server:parsing`. Domain coverage gate: 90% lines (Kover).
- Invariants run on random inputs with a fixed seed: allocations sum to the line amount, period balances sum to zero,
  simplified transfers settle in at most n − 1 transfers.
- Parser golden tests read redacted text fixtures (`fixtures/invoices/**/*.redacted.txt`) and compare to expected
  JSON snapshots.
- ViewModels: every event and every state (loading, empty, error, content) with fake repositories from `core:testing`.
- Server routes: Ktor `testApplication`; persistence with Testcontainers PostgreSQL.
- Design system: every component state in `catalogEntries` gets a Roborazzi screenshot in light and dark.
- The end-to-end demo scenario (design-plan section 8) must pass before a milestone is tagged.

## Git workflow

- Conventional Commits (`feat(bills): add approval flow`), small and focused, with the reason in the body.
- Work happens on the designated branch (ADR 0008); each milestone ends with a tag `mN-complete`.
- Run the `pre-commit-review` skill before every commit.

## Definition of done

A change is done when:

1. `./gradlew check` passes locally, and CI (`check` and `ios` jobs) is green on the pushed commit.
2. New logic has tests; domain and parser code was written test-first; coverage gates hold.
3. It matches `docs/design-plan.md`, or the plan and an ADR were updated in the same change.
4. Screens handle loading, empty, error-with-retry and content states, use only design-system components and string
   resources, and meet the accessibility rules (48 dp targets, AA contrast, content descriptions, 200% font scale).
5. Screenshot goldens changed only intentionally and were reviewed.
6. No secrets, PII, real invoices, debug leftovers or TODOs in the diff.

## Skills

`.claude/skills/` holds step-by-step guides: `new-feature-module`, `add-invoice-parser`, `add-design-component`,
`write-adr`, `pre-commit-review`.
