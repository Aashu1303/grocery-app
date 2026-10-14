# Contributor reference

[← Visual overview](../README.md)

This guide describes the current `main` implementation. Read the complete [design plan](design-plan.md) and [AGENTS.md](../AGENTS.md) before making changes. The [milestone record](milestones.md) carries historical verification evidence; it is not a fresh check of your environment.

## Source map

| Layer | Modules | Responsibilities |
| --- | --- | --- |
| Platform entry | `androidApp`, `iosApp` | Android Application/Activity, iOS SwiftUI wrapper, platform authentication and configuration |
| App composition | `composeApp` | Routes, application state, Koin graph and iOS framework |
| Feature UI | `feature:household`, `pantry`, `invoiceimport`, `bills`, `ledger`, `kitchen` | Screen, ViewModel, immutable state, events and one-off effects |
| Core types | `core:model` | Money, quantities, IDs, entities, `AppResult` and `AppError` |
| Business rules | `core:domain` | Use cases, repository interfaces and pure algorithms |
| Data adapters | `core:data` | HTTP repositories, DTO mapping, SQLDelight cache and platform drivers |
| API contract | `core:contract` | Serialized DTOs and shared JSON configuration |
| UI system | `core:designsystem` | Semantic tokens, theme, controls, brand components and catalog |
| Test support | `core:testing` | Fakes, synthetic builders and test helpers; test dependencies only |
| Backend | `server:app` | Routes, auth/session verification, role checks, services, persistence and migrations |
| Invoice parsing | `server:parsing` | Positioned PDF text, detection, retailer templates, normalization, reconciliation and LLM fallback |
| Scheduled work | `server:jobs` | Job runner; business sweeps are registered by `server:app` |
| Notifications | `server:notifications` | Notification abstraction and FCM delivery |
| Build infrastructure | `build-logic`, `build-logic-android` | Compiled convention plugins, quality checks and platform configuration |

Gradle paths use colons: `core:domain` is the directory `core/domain`. The package root is `app.rasoi`. Android is a separate application module under AGP 9; use `:androidApp:assembleDebug`, not an application build task on `composeApp`.

## Follow a change through the layers

For a new screen action:

1. Define the event and expected UI state transitions in its feature module.
2. Add or reuse a domain use case and repository interface. Write domain tests first when behavior changes.
3. Implement boundary work in data or server adapters; map failures to `AppError`.
4. Wire dependencies and cross-feature navigation in `composeApp`.
5. Cover loading, empty, error and content states with fake repositories.
6. Verify relevant behavior, module rules and intentional screenshot changes.

Do not import another feature or `core:data` from feature code. Do not put HTTP, database, UI or platform types into domain logic. Shared app/server contracts do not expose persistence entities.

Use `…Screen`, `…ViewModel`, `…UiState`, `…UiEvent`, `…Repository`, `…UseCase`, `…Dto` and `…Entity` suffixes. Hoist composable state and put `Modifier` first among optional parameters. User-visible strings belong to the owning module's resources; feature controls use the design system.

## Financial invariants

| Concern | Required behavior |
| --- | --- |
| Money | `Money` backed by `Long` paise; exact parsing and arithmetic |
| Quantities | Integer base units with `QuantityUnit` |
| Allocation | One person, equal or quantity; largest-remainder rounding; exact sum |
| Charges | Signed fees/discounts distributed without losing paise |
| Ledger | Approved bills credit the payer and debit shares; sum of balances is zero |
| Settlement | Match creditors/debtors; at most `n - 1` transfers |
| Time | Inject `kotlin.time.Clock`; UTC instants with household-zone month boundaries |
| Consistency | Server recomputes final results; authorization and idempotency at mutation boundaries |

See [bill workflow](adr/0021-splits-approvals-and-scheduled-work.md) and [ledger decisions](adr/0022-ledger-and-settlement.md). A pantry item is created at import, independently of approval; [ADR 0020](adr/0020-pantry-from-bills.md) explains food overrides and editing behavior.

## Build and runtime reference

Use JDK 21 as in CI and the repository wrapper. Exact pins live in [the version catalog](../gradle/libs.versions.toml); Gradle itself is pinned in [wrapper properties](../gradle/wrapper/gradle-wrapper.properties).

| Area | Current pin |
| --- | --- |
| Gradle | 9.8.0 |
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.12.1 |
| Android Gradle Plugin | 9.4.1 |
| Ktor | 3.6.0 |
| Koin / compiler plugin | 4.2.2 / 1.2.1 |
| SQLDelight | 2.4.1 |

`rasoi.android` and `rasoi.compose` are enabled in repository properties; iOS defaults to macOS. Disabling a target helps limited environments but does not satisfy verification of that platform.

The [local runtime guide](running-locally.md) owns full setup and deployment commands. Key configuration:

| Setting | Purpose |
| --- | --- |
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | PostgreSQL connection; required at server startup |
| `RASOI_AUTH_MODE` | Defaults to Firebase; `dev` is local-only |
| `FIREBASE_PROJECT_ID` | Required for Firebase token verification |
| `PORT` | Server port, default 8080 |
| `RASOI_STORAGE_DIR` | Private PDF folder, default `data/documents` |
| `RASOI_LLM_BASE_URL`, `RASOI_LLM_API_KEY`, `RASOI_LLM_MODEL` | Optional LLM; set all three or none |
| `FCM_SERVICE_ACCOUNT_FILE` | Optional server push credentials |
| `RASOI_SWEEP_TOKEN` | External scheduler authentication; enables externally triggered sweeps instead of the timer |
| Android `-Prasoi.apiBaseUrl` | Build-time API URL; HTTPS required in release |
| iOS `RASOI_API_BASE_URL` | Wrapper API URL configuration |

`GET /health` returns status and version. Server startup opens the database and applies migrations; this is no longer a standalone foundation liveness service. Hosted configuration uses Cloud Run, Neon and mounted Cloud Storage, as described in [ADR 0028](adr/0028-hosting-on-cloud-run-and-neon.md).

## Verification and review

Run `./gradlew check` for configured targets and the focused tests relevant to your change. Server tests need Docker for Testcontainers. `verifyModuleGraph` enforces module edges; `verifyFeatureSources` enforces feature UI rules. Resource checks run with `python3 scripts/check-compose-resources.py`.

Screenshots use Compose Desktop/Roborazzi and `verifyRoborazziJvm`. Goldens are Linux-rendered; the self-hosted macOS CI fallback skips that comparison. Review captures before accepting new goldens; never record simply to hide failures. See [CI](ci.md) and [milestone limitations](milestones.md).

Use the repository [pre-commit review skill](../.claude/skills/pre-commit-review/SKILL.md). Check plan conformance, inward dependencies, exact-money rules, privacy, UI states, naming and scope. Record unavailable checks explicitly, then inspect CI for the PR head before merging.

This guide introduces no architecture change or new dependency. Runtime and provider validation remain distinct from source inspection, compilation and automated tests.
