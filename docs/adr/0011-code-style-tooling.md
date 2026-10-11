# 0011. Code style tooling

Status: Accepted
Date: 2026-10-10

## Context
The plan requires the official Kotlin coding conventions plus the Android Kotlin style guide, enforced in CI.

## Decision
- ktlint (Gradle plugin `org.jlleitschuh.gradle.ktlint`) with `ktlint_code_style = intellij_idea`, which implements
  the official Kotlin conventions; 120-column lines; trailing commas; Composable functions exempt from the
  function-naming rule. Configured in `.editorconfig`.
- detekt 1.23 with `config/detekt/detekt.yml` on top of the defaults, zero tolerated issues. It forbids `!!`,
  `GlobalScope`, unsafe casts, magic numbers and TODO/FIXME comments.
- Root task `verifyFeatureSources` rejects raw colours, raw `dp`/`sp`, direct Material imports and hard-coded
  `Text("...")` strings in `feature/*`. Root task `verifyModuleGraph` enforces the module dependency rules.
- `./gradlew check` runs all of the above with the tests.

## Consequences
Style is never debated in review. detekt 1.23 is built against an older Kotlin; if a Kotlin upgrade breaks it, move
to detekt 2 and record that here.
