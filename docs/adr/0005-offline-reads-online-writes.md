# 0005. Offline reads, online-only writes in Phase 1

Status: Accepted
Date: 2026-10-10

## Context
Offline conflict resolution for shared bills and ledgers is a large problem outside Phase 1 scope.

## Decision
SQLDelight caches the pantry and ledger for offline reads. All writes require connectivity and go to the server.

## Consequences
Simple, consistent behaviour. Revisit with a write queue if offline use proves important.
