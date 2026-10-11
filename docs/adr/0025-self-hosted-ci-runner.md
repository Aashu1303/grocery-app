# 0025. CI on a self-hosted macOS runner

Status: Accepted
Date: 2026-10-11

## Context
The repository's GitHub-hosted Actions minutes ran out, so every job failed before its first step. The owner added a
self-hosted runner: macOS, Apple silicon, labels `self-hosted`, `macOS`, `ARM64`.

## Decision
- All CI and screenshot-recording jobs run on that runner. The runner is chosen by the repository variables
  `RASOI_RUNNER` and `RASOI_IOS_RUNNER` (JSON label lists), defaulting to the self-hosted labels, so switching back to
  hosted runners needs no commit.
- Jobs install what a workflow can (Java 21, Android SDK, `xcodegen`) and check what it cannot (Docker for
  Testcontainers, Xcode), failing with a message that points to `docs/ci.md`.
- On macOS Gradle turns the iOS targets on by default (ADR 0007), so the JVM/Android jobs pass `-Prasoi.ios=false`; the
  iOS job alone builds Kotlin/Native.
- Screenshot goldens are rendered and verified on the same runner type. They were recorded on Linux before this
  change, so one `[record screenshots]` commit re-records all of them.

## Consequences
- One runner executes the jobs one after another; a full run is slower than three parallel hosted jobs.
- The machine runs repository code, so the repository must be private or gate outside contributors (`docs/ci.md`).
- Moving to another OS later means re-recording the goldens again.
