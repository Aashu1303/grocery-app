# Grocery App: Phase 1 Design Plan

Oct 11, 2026 · @Aashutosh

## 1. Overview

Phase 1 builds a Kotlin Multiplatform app (Android and iOS) plus a Kotlin backend that lets a shared flat import grocery invoice PDFs, keep a live pantry, avoid waste, get dish ideas from stock, and split bills fairly with month-end settlement. This plan is the input for development with Claude Code: export it to Markdown and commit it as `docs/design-plan.md`.

**Phase 1 scope**

- Household setup with roles: owner, member, cook
- Pantry: item, location, quantity, price, payer
- PDF invoice import (Zepto, Blinkit, Instamart, Handpickd) plus manual add
- Expiry estimates, "use first" list, expiry alerts
- Dish suggestions from current stock
- Bill split flow: payer, per-item allocation, approval, auto-approve, edit until month lock
- Month-end settlement with simplified debts and UPI settle-up

**Out of scope for Phase 1:** voting, cook WhatsApp notifications, cooking preferences, shopping suggestions, marketplace, barcode scanning (moved to the end of Phase 1 as a stretch goal).

**Milestones**

| # | Milestone | Done when |
| --- | --- | --- |
| M0 | Foundation | Repo, modules, CI, design system catalog, AGENTS.md, skills; empty app runs on Android and iOS |
| M1 | Household and auth | Sign-in, create household, invite members, roles |
| M2 | Invoice import | All four sample formats parse to the canonical model with reconciliation; review screen |
| M3 | Pantry | Bill lines become pantry items with location, category and expiry estimate; manual add and edit |
| M4 | Bill split and approvals | Allocation, approval flow, auto-approve job, edits with re-approval |
| M5 | Ledger and settlement | Month close, debt simplification, UPI deep link, mark as settled |
| M6 | Kitchen | Use-first list, expiry notifications, dish suggestions from stock |
| M7 | Hardening | End-to-end demo scenario passes, accessibility pass, release builds |

## 2. What the sample invoices tell us

All four samples have a text layer, so text extraction works without OCR, but each platform uses a different layout, unit model and discount format. One canonical model with four template parsers is the right design.

| Platform | Seller on invoice | Line columns that matter | Quantity model | Discount | Fees on invoice | Line totals reconcile |
| --- | --- | --- | --- | --- | --- | --- |
| Instamart | Swinsta Ent Pvt Ltd (franchise) | Description, Qty, UQC, HSN, Net taxable, GST, Total | Count (`NOS`) | Absolute, excl. tax | Handling fee 12.01 | 457 + 12.01 = 469.01 |
| Zepto | Geddit Convenience Pvt Ltd | Item, MRP, HSN, Qty, Rate, Disc %, Taxable, GST, Total | Count; pack size in the name ("Lemon 200 g") | Percentage | None on this invoice | 866.99 + 0.01 round-off = 867.00 |
| Blinkit | Blink Commerce Pvt Ltd | UPC, Description, MRP, Discount, Qty, Taxable, GST, Total | Count | Absolute vs MRP | None on this invoice | 129.00 |
| Handpickd | Handpickd (not a GST invoice) | Item, rate per kg, weight, amount, promo discount | Measured weight (470 g at 43/kg) | Per-line promo, plus delivery discount | Delivery 19, delivery discount 10, pesticide-free 15 | 114 - 8 + 15 + 19 - 10 = 130 |

**Parser requirements these samples force**

- **Platform detection** by seller and footer signatures ("Swinsta", "Blink Commerce", "ZEPTO MARKETPLACE", "handpickd"), not by file name.
- **Wrapped descriptions:** item names span several lines (Instamart, Zepto), so rows are rebuilt from text positions, not split by newline.
- **Multi-page tables** with repeated headers (Zepto).
- **Zero-value promo lines** must be dropped (Instamart "Flyer - Nat Habit Shampoo", total 0).
- **Non-food items** appear (Blinkit hot/cold pack). They are splittable but never enter the pantry.
- **Split basis** = the amount actually paid per line: tax-inclusive Total for GST invoices; amount minus line promo for Handpickd (20 - 1 = 19).
- **Bill-level charges and discounts** (handling, delivery, extras, delivery discount) are separate charge lines, distributed by the split rules.
- **Reconciliation check:** lines + charges - discounts + round-off must equal the invoice total, or the bill is flagged on the review screen. All four samples pass this check.
- **HSN code as a category signal:** chapter 07/08 produce, 04 dairy, 19/21 packaged food, 22 beverages, 39 and 49 non-food. Use it for category, default location and expiry estimate.
- **Quantity parsing** from three sources: count columns, pack size in names ("pack (250 g)", "900 g - 1 kg"), and measured weight.
- **Fees on separate invoices:** Zepto and Blinkit showed no fees here. If an order had fees they may arrive as a separate PDF, so one bill accepts several PDFs plus manual charge lines. Confirm with a fee-bearing order.
- **Duplicate guard:** platform + invoice number is unique per household.

**Privacy:** invoices carry your name and home address. Real PDFs stay in a git-ignored `fixtures/invoices/` folder. Committed tests use text fixtures with names and addresses replaced, and only line-item text is ever sent to an LLM.

## 3. Engineering principles

Most of the logic lives in shared, pure Kotlin that is tested on the JVM; platform code stays thin and sits only at the edges.

**Architecture**

1. **Layers:** UI (Compose) → ViewModel → use cases (domain) → repositories (data). Dependencies point inward only; the domain imports nothing from data, UI or any platform.
2. **Unidirectional data flow (MVI):** each screen exposes one immutable `UiState` as a `StateFlow` and accepts sealed `UiEvent`s. One-off effects (navigate, toast) go through a separate channel.
3. **Maximise `commonMain`:** `expect`/`actual` only for file picking, PDF sharing intents, notifications and secure storage. Wrap each behind an interface in common code.
4. **Feature modules** are independent: a feature never depends on another feature, only on `core` and `designsystem`.
5. **Shared contract module:** request and response DTOs are defined once and used by both the app and the server.
6. **The server is the source of truth** for bills, approvals and the ledger. The same domain algorithms (split, settlement) run on both sides, but only server results are final.

**Correctness**

1. **Money is `Long` paise.** Never `Float` or `Double`. One `Money` value class with arithmetic and formatting.
2. **Quantities are `Long` base units** (milligrams, millilitres, or count × 1000) with a `QuantityUnit` enum (named to avoid `kotlin.Unit`; ADR 0002).
3. **Splits always sum exactly** to the source amount, using largest-remainder rounding. Tested as an invariant.
4. **Time:** store UTC `Instant`s with `kotlinx-datetime`; convert to the household time zone only for display and month boundaries.
5. **Errors are values:** repositories return a sealed `Result`-style type with typed failures. No exceptions cross layer boundaries.
6. **Immutability by default:** `data class` + `val`, persistent updates via `copy`.
7. **Idempotent writes:** every mutating request carries an idempotency key, so retries never double-post a bill.

**Security and privacy**

1. Every endpoint checks household membership and role before any read or write.
2. Secrets come from environment variables; nothing secret is committed.
3. No PII in logs. The LLM receives line-item text only, never header blocks with names or addresses.
4. Invoice PDFs are stored privately per household, with a retention limit.

**Maintainability**

1. Small files and small functions; one public class per file.
2. Architecture decisions recorded as ADRs in `docs/adr/`.
3. Strings in resources from day one (Hindi for the cook comes later).
4. No new dependency without a one-line justification in the PR or commit message.

## 4. Tech stack and key decisions

The whole system is Kotlin, so the app and server share models and domain logic. Pin exact versions in `gradle/libs.versions.toml` when the project is created.

| Area | Choice | Why |
| --- | --- | --- |
| Shared UI | Compose Multiplatform (Android, iOS) | One UI codebase; desktop and web possible later |
| Navigation | Navigation Compose for Compose Multiplatform | Official, type-safe routes |
| DI | Koin | KMP-native, simple, no code generation |
| Networking | Ktor client + kotlinx.serialization | Multiplatform, same serialization as the server |
| Local storage | SQLDelight | Typed SQL on Android and iOS; offline reads of pantry and ledger |
| Backend | Ktor server (JVM) | Kotlin end to end; shares `contract` and `domain` modules |
| Database | PostgreSQL + Exposed, migrations with Flyway | Relational data (ledger, allocations) needs constraints and transactions |
| PDF parsing | Apache PDFBox on the server, text with positions | JVM-native; positions rebuild wrapped table rows; keeps parsers in one place |
| LLM fallback and dish suggestions | OpenAI-compatible API (for example Groq) called from the server | API keys never ship in the app; one swappable client |
| Auth | Firebase Auth phone OTP; server verifies the ID token | Phone numbers fit Indian flatmates and cooks |
| Push | Firebase Cloud Messaging (Android, and iOS via APNs) | One channel for approvals and expiry alerts |
| File storage | S3-compatible object storage | Invoice PDFs for re-parsing and dispute evidence |
| Scheduled jobs | In-server scheduler with a jobs table | Auto-approve, expiry alerts, month close; no extra infrastructure |
| Payments | UPI deep link (`upi://pay`) + manual "mark settled" | No payment processing or licensing needed |
| Build | Gradle version catalog + convention plugins | Consistent module setup; every shared module also has a `jvm` target for fast local tests (ADR 0007) |
| CI | GitHub Actions | Build, lint, test on every push |

**Decisions to record as ADRs at M0:** server-side PDF parsing, money as paise, server-authoritative ledger, Firebase for auth and push, offline reads with online-only writes in Phase 1.

**Open question:** where to host the server and database (for example Railway, Render, Fly.io or a small VPS). It does not block M0 to M2, since local development runs on Docker Compose.

## 5. High-level design (HLD)

The app talks only to the Ktor server; the server alone reads and writes PostgreSQL, stores invoice PDFs, and calls Firebase and the LLM.

&#91;embedded content: system architecture · app, server, stores\]

**Invoice import, end to end:** the app uploads PDFs, the server extracts, parses and reconciles them, stores the PDFs, and returns a draft bill. The user reviews and allocates in the app, the server records approvals, and jobs handle auto-approval, expiry alerts and month close.

## 6. Low-level design (LLD)

The LLD fixes the module layout, schema, bill lifecycle, algorithms, parser pipeline and API so each milestone can be built and tested independently.

### 6.1 Module layout

```text
composeApp/          Android + iOS entry, navigation graph, DI wiring
iosApp/              Xcode wrapper
core/model/          entities, Money, Quantity, typed ids
core/domain/         use cases: split, settlement, expiry, categorizer
core/data/           repositories, Ktor client, SQLDelight
core/contract/       DTOs shared with the server
core/designsystem/   tokens, theme, components, catalog screen
core/testing/        fakes, builders, fixture loaders
feature/household/   setup, invites, roles
feature/pantry/      list, locations, item detail, manual add
feature/invoiceimport/ pick PDF, parse review
feature/bills/       bill editor, allocation, approvals
feature/ledger/      balances, month close, settle up
feature/kitchen/     use-first list, dish suggestions
server/app/          Ktor entry, routing, auth, DI
server/parsing/      extraction, detection, template parsers, LLM fallback
server/jobs/         auto-approve, expiry alerts, month close
server/notifications/ FCM
fixtures/invoices/   redacted text fixtures (committed; real PDFs git-ignored, ADR 0009)
build-logic/         convention plugins and module/design-system rule checks (ADR 0010)
build-logic-android/ Android convention plugins, used when rasoi.android=true (ADR 0007)
docs/                design-plan.md, kickoff-prompt.md adr/
```

### 6.2 Data model

| Table | Key columns | Rules |
| --- | --- | --- |
| users | id, phone, display\_name, upi\_vpa | UPI ID optional until settle-up |
| households | id, name, time\_zone | Time zone sets month boundaries |
| memberships | household\_id, user\_id, role | Role: OWNER, MEMBER, COOK; unique per pair |
| invites | code, household\_id, role, expires\_at | Single use |
| locations | id, household\_id, name, kind | Kind: FRIDGE, FREEZER, SHELF, OTHER |
| bills | id, household\_id, platform, invoice\_no, order\_id, invoice\_date, payer\_id, total\_paise, status, period\_id | Unique (household\_id, platform, invoice\_no) |
| bill\_documents | bill\_id, storage\_key, sha256 | Several PDFs per bill |
| bill\_lines | id, bill\_id, raw\_name, display\_name, quantity, unit, amount\_paise, hsn, upc, category, is\_food | amount\_paise = what was paid for the line |
| bill\_charges | id, bill\_id, kind, amount\_paise, distribution | Kind: HANDLING, DELIVERY, DISCOUNT, ROUND\_OFF, OTHER; signed amount |
| allocations | line\_id, user\_id, mode, quantity, amount\_paise | Amounts stored after computation; sum = line amount |
| approvals | bill\_id, user\_id, state, comment, decided\_at | State: PENDING, APPROVED, CHANGES\_REQUESTED |
| periods | id, household\_id, month, status | OPEN or LOCKED |
| settlements | id, period\_id, from\_user, to\_user, amount\_paise, status | PENDING or PAID |
| pantry\_items | id, household\_id, name, category, quantity, unit, location\_id, expires\_on, status, source\_line\_id | Status: IN\_STOCK, FINISHED, DISCARDED |
| jobs | id, kind, run\_at, payload, status | Survives restarts |
| device\_tokens | user\_id, token, platform | For FCM |

### 6.3 Bill lifecycle

A bill moves from draft to locked through approval; only approved bills count toward a month's settlement.

&#91;embedded content: bill lifecycle · 5 states\]

A change request loops the bill back to its uploader; an edit to an approved bill also returns it to pending, for affected members only.

- **Affected members** = everyone with a non-zero share, excluding the payer if the payer submitted.
- **Auto-approve:** 48 hours after submission, remaining PENDING approvals become APPROVED by the system.
- **Edits after approval** reset approval only for members whose share changed.
- **Month close** (1st of the month, household time zone): the previous period locks; bills not yet approved move to the new period.

### 6.4 Core algorithms

1. **Line allocation.** ONE: the whole amount to one member. EQUAL: split among chosen members. QUANTITY: amount × member units ÷ line units; allocated units must equal line units.
2. **Exact rounding.** Every split uses largest-remainder: floor each share in paise, then hand the leftover paise to the largest fractional parts. Shares always sum to the source amount.
3. **Charges and bill discounts.** Default PROPORTIONAL to each member's line total on that bill; EQUAL as an option. Negative charges use the same rule.
4. **Period balance.** For each approved bill, the payer is credited the total and each member is debited their allocations plus charge shares. Invariant: balances sum to zero.
5. **Debt simplification.** Repeatedly match the largest creditor with the largest debtor; at most n - 1 transfers for n members.
6. **Categorizer.** HSN chapter gives the category and `is_food`; keyword rules override. Category sets default location (dairy to fridge) and expiry.
7. **Expiry estimate.** A config table of default shelf life per category (for example milk 2 days, leafy vegetables 3), editable per item.
8. **Use-first ranking.** In-stock food items sorted by days to expiry, then by quantity.

### 6.5 Invoice parsing pipeline

1. **Extract** text with positions (PDFBox), all pages.
2. **Detect** the platform from seller and footer signatures.
3. **Parse** with that platform's template into `ParsedInvoice`: header, lines, charges, total.
4. **Fall back** to the LLM when no template matches or a template fails; validate its JSON against the same schema.
5. **Normalize:** clean names, parse quantity and unit, categorize, drop zero-value lines.
6. **Reconcile** lines + charges + round-off against the invoice total; mismatches become warnings.
7. **Return** a draft bill with warnings and a confidence level for the review screen.

```kotlin
interface InvoiceParser {
    val platform: Platform
    fun matches(document: ExtractedDocument): Boolean
    fun parse(document: ExtractedDocument): ParseResult
}
```

### 6.6 API (Phase 1)

| Method | Path | Purpose |
| --- | --- | --- |
| POST | /auth/session | Exchange a Firebase ID token for a session |
| POST | /households | Create a household |
| POST | /households/{id}/invites | Create an invite |
| POST | /invites/{code}/accept | Join a household |
| GET, POST | /households/{id}/locations | List and add locations |
| GET, POST, PATCH | /households/{id}/pantry | List, add and update pantry items |
| POST | /households/{id}/invoices | Upload one or more PDFs; returns a parsed draft bill |
| GET, POST | /households/{id}/bills | List by period; create a manual bill |
| PATCH | /bills/{id} | Edit lines, payer, allocations, charges |
| POST | /bills/{id}/submit | Send for approval |
| POST | /bills/{id}/approvals | Approve or request changes |
| GET | /households/{id}/ledger?period= | Balances and suggested transfers |
| POST | /households/{id}/periods/{month}/close | Close a month early (owner only) |
| POST | /settlements/{id}/paid | Mark a transfer as paid |
| GET | /households/{id}/kitchen/use-first | Use-first list |
| POST | /households/{id}/kitchen/suggestions | Dish ideas from current stock |
| POST | /devices | Register an FCM token |

## 7. Design system and UI rules

All UI is built from one `core/designsystem` module: features use its tokens and components only, never raw colours, sizes or Material components directly.

### 7.1 Tokens

| Token group | Values | Rule |
| --- | --- | --- |
| Colour (semantic) | primary, onPrimary, surface, surfaceVariant, onSurface, outline, success, warning, danger, owedToYou, youOwe | Light and dark sets; named by meaning, not hue |
| Freshness | fresh, useSoon, expired | Mapped to success, warning, danger |
| Typography | display, title, body, label, numeric | `numeric` uses tabular figures for amounts |
| Spacing | 4, 8, 12, 16, 24, 32, 48 dp | 4 dp grid; screen padding 16 |
| Radius | 8, 12, 16 dp | Cards 12, sheets 16 |
| Elevation | 0, 1, 3 | Mostly flat; elevation only for sheets and FABs |
| Motion | 150 ms and 250 ms, standard easing | No decorative animation |

`AppTheme` wraps Material 3 and exposes tokens through `AppTheme.colors`, `AppTheme.type` and `AppTheme.spacing`.

### 7.2 Components

| Component | Used for |
| --- | --- |
| AppButton (primary, secondary, text, destructive) | All actions |
| AppCard | Grouped content |
| AppTopBar, AppBottomBar | Screen chrome |
| AmountText | Rupee amounts in Indian grouping (₹1,23,456.00), sign + colour + label for owed or owing |
| ExpiryChip | Fresh / use soon / expired, always with text, never colour alone |
| PantryItemRow | Name, quantity, location, expiry chip |
| BillLineRow | Line name, amount, allocation summary |
| SplitSelector | One person / equal / by quantity per line |
| QuantityStepper | Quantity edits |
| MemberAvatar, MemberPicker | People in a household |
| ApprovalBanner | Pending, approved, changes requested |
| StateViews: Loading (skeleton), Empty, Error | Every screen's non-content states |
| AppBottomSheet, ConfirmDialog | Pickers and destructive confirmation |

Every component appears in a **catalog screen** (debug builds) in all states, light and dark, and each has screenshot tests.

### 7.3 UI rules

1. **Navigation:** bottom bar with Kitchen (home: use-first and suggestions), Pantry, Bills, Balances; household and settings from the avatar.
2. **Every screen** renders four states: loading, empty, error with retry, content.
3. **One primary action** per screen.
4. **Touch targets** at least 48 dp; text contrast meets WCAG AA; all icons have labels or content descriptions.
5. **Amounts** are right-aligned with tabular figures; owed/owing shown by sign and word, not colour alone.
6. **Dates** as "12 Oct"; relative for expiry ("2 days left").
7. **Destructive actions** confirm first; reversible ones offer undo.
8. **Copy** is short, plain English; buttons are verbs ("Approve split", not "OK").
9. **Forms** validate inline and keep the user's input after an error.
10. **Dynamic type** up to 200% without clipping; layouts scroll instead of truncating amounts.

## 8. Testing strategy

Tests run on every commit, and no change merges without passing them; money and parsing code are test-first.

| Layer | Tools | What is tested | Gate |
| --- | --- | --- | --- |
| Domain (`core/domain`) | kotlin.test | Split, rounding, charges, balances, debt simplification, expiry, categorizer | 90% line coverage; property-style invariant tests |
| Parsers (`server/parsing`) | kotlin.test + golden fixtures | Each platform's redacted fixture parses to an expected JSON snapshot; reconciliation passes | Every fixture green |
| ViewModels | kotlin.test + Turbine + fake repositories | State transitions for every event, including loading, empty and error | Every screen state covered |
| Data layer | Ktor MockEngine, SQLDelight in-memory driver | Mapping, caching, error handling |  |
| Server routes | Ktor `testApplication` + Testcontainers PostgreSQL | Auth, membership checks, bill lifecycle, idempotency | Every endpoint |
| Jobs | Fake clock | Auto-approve at 48 h, month close at the household's midnight |  |
| UI | Roborazzi screenshot tests | Every design-system component and screen state, light and dark | Reviewed diffs only |
| End to end | Seeded demo household | Full scenario below | Must pass before each milestone closes |

**Invariants tested on random inputs**

- Allocations always sum to the line amount, to the paise.
- Period balances always sum to zero.
- Simplified transfers settle every balance in at most n - 1 transfers.

**End-to-end demo scenario** (the "test scene" that touches every feature and endpoint)

1. Two flatmates and a cook sign in; one creates a household and invites the others.
2. Import all four redacted invoices, including one order with two PDFs; resolve one reconciliation warning.
3. Allocate lines with each split mode; set a non-food item to one person.
4. Submit; one member approves, one requests a change; edit and re-approve; one bill auto-approves on a fake clock.
5. Pantry fills from food lines; move items between locations; mark items finished and discarded.
6. Advance the clock: expiry alerts fire, the use-first list reorders, dish suggestions use only in-stock items.
7. Upload a duplicate invoice and confirm it is rejected.
8. Close the month; verify balances, simplified transfers and a UPI link; mark a transfer paid.
9. Confirm a non-member cannot read or change any of it.

## 9. Repo conventions and workflow

Code follows the official Kotlin coding conventions, enforced by ktlint and detekt in CI, so style is never debated in review.

**Code style**

- Kotlin coding conventions (kotlinlang.org) plus the Android Kotlin style guide for Compose.
- Naming: `PascalCase` types and composables, `camelCase` functions and properties, `SCREAMING_SNAKE_CASE` constants, packages `lowercase` by feature (`app.<name>.feature.bills`).
- Suffixes: `…Screen`, `…ViewModel`, `…UiState`, `…UiEvent`, `…Repository`, `…UseCase`, `…Dto`, `…Entity`.
- Composables: stateless by default, state hoisted; `Modifier` is the first optional parameter; previews for every screen state.
- No comments that restate code; KDoc only on public domain APIs and non-obvious algorithms.
- No `!!`, no `GlobalScope`, no hard-coded strings or colours in features.

**Git**

- Conventional Commits (`feat(bills): add approval flow`), small and focused.
- `main` is always green; work on short-lived branches named `m2/invoice-parsing` (Phase 1 is built on a single designated branch; ADR 0008).
- Each milestone ends with a tag (`m2-complete`).

**Review before every commit**

1. Build passes; ktlint and detekt clean.
2. All tests pass; new logic has tests; coverage gates hold.
3. Change matches this plan or updates the plan and an ADR.
4. No secrets, PII, real invoices or debug leftovers in the diff.
5. Design-system rules followed; screenshot diffs reviewed.
6. Error, empty and loading states handled.

**Agent support files**

- `AGENTS.md`: project overview, module map, build and test commands, the rules above, definition of done.
- Skills in `.claude/skills/`: `new-feature-module`, `add-invoice-parser`, `add-design-component`, `write-adr`, `pre-commit-review`.