---
name: pre-commit-review
description: Mandatory review before every commit in Rasoi, using the design-plan section 9 checklist. Run it on the staged diff and fix every finding before committing.
---

# Pre-commit review

Review the staged diff (`git diff --cached`) and fix every problem before committing. Report anything you cannot
verify locally (for example Compose or iOS on a restricted machine) and check CI after pushing.

## 1. Build, lint and tests

- `./gradlew check` passes: ktlint, detekt, all tests, Kover gate, `verifyModuleGraph`, `verifyFeatureSources`.
- On a machine without Compose support, also run
  `./gradlew -Prasoi.compose=true <changed compose modules>:ktlintCheck <…>:detekt`.
- New logic has tests; domain and parser changes were written test-first; invariant tests still pass.

## 2. Plan conformance

- The change matches `docs/design-plan.md` (modules, data model, API, algorithms, UI rules).
- Deviations: the plan is updated and an ADR is added (`write-adr` skill).

## 3. Correctness and architecture

- Money is `Money`/Long paise and quantities are `Quantity`; no `Float`/`Double`; splits use largest-remainder.
- Errors are `AppResult`/`AppError`; no exception crosses a layer; no swallowed errors.
- Dependencies point inward; features do not import other features or `core:data`.
- Server: every endpoint checks membership and role; mutating requests are idempotent.
- Time uses the injected `Clock`; household time zone only at the edges.

## 4. Security and privacy

- No secrets, tokens or credentials; configuration comes from environment variables.
- No PII in logs, fixtures or test names. `./scripts/check-no-real-invoices.sh` passes.
- No real invoice files staged: `git diff --cached --name-only | grep fixtures/invoices` shows only
  `*.redacted.txt` or the README.

## 5. UI

- Only design-system components, tokens and string resources in features.
- Loading, empty, error-with-retry and content states exist for every screen.
- 48 dp touch targets, content descriptions, colour never the only signal, layouts survive 200% font scale.
- Screenshot golden changes are intentional and reviewed.

## 6. Style and hygiene

- Naming and suffix rules from AGENTS.md; one public class per file; `Modifier` first optional parameter.
- No `!!`, `GlobalScope`, TODO/FIXME, commented-out code, debug prints or stray files.
- Comments explain why, not what; KDoc on public domain APIs only.
- New dependencies have a one-line justification in `libs.versions.toml` and the commit message.

## 7. Commit

- Conventional Commit subject (`type(scope): summary`), body explains why, with the required trailers.
- One logical change per commit.
