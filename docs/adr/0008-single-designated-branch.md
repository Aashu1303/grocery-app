# 0008. Work on the designated branch, tag milestones

Status: Accepted
Date: 2026-10-10

## Context
Section 9 of the plan suggests short-lived milestone branches. This session is bound to a single branch.

## Decision
All work is committed to `claude/compassionate-gates-fojlox`. Each milestone ends with a tag such as `m0-complete`. Commits are Conventional Commits and each passes the pre-commit review.

## Consequences
Linear history per milestone. Switch to per-milestone branches when several people work in parallel.
