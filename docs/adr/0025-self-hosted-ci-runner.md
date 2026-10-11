# 0025. Self-hosted runner as the CI fallback

Status: Accepted
Date: 2026-10-11

## Context
The repository's GitHub-hosted Actions minutes ran out, so every job failed before its first step. The owner added a
self-hosted runner (macOS, Apple silicon, labels `self-hosted`, `macOS`, `ARM64`) and wants it used only when
GitHub-hosted runners are not available.

## Decision
- CI normally runs on GitHub-hosted runners. A first job, `choose-runner`, runs on `ubuntu-latest`; every other job
  `needs` it, runs even if it failed (`if: !cancelled()`) and takes its runner from its outputs. If the hosted runner
  could not start, the outputs are empty and the jobs use the self-hosted labels. No manual switch is needed.
- The repository variables `RASOI_RUNNER` and `RASOI_IOS_RUNNER` (JSON label lists) force a choice, for the case where
  hosted jobs queue forever instead of failing.
- Screenshot goldens are Linux renderings. Roborazzi compares pixels and macOS renders text differently, so the
  self-hosted fallback skips `verifyRoborazziJvm` (with a notice), and the recorder workflow always runs on
  `ubuntu-latest`. Goldens for new screens therefore wait for hosted runners; until then they are missing, not wrong.
- Jobs install what a workflow can (Java 21, Android SDK, `xcodegen`) and check what it cannot (Docker for
  Testcontainers, Xcode), failing with a message that points to `docs/ci.md`.
- On macOS Gradle turns the iOS targets on by default (ADR 0007), so JVM/Android jobs pass `-Prasoi.ios=false`.

## Consequences
- While hosted minutes are unavailable, CI still compiles and tests everything, but does not compare screenshots.
- One runner executes jobs one after another, so a fallback run is slower than three parallel hosted jobs.
- The fallback machine runs repository code; keep the repository private or gate outside contributors (`docs/ci.md`).
