# 0003. The server is the source of truth for bills and the ledger

Status: Accepted
Date: 2026-10-10

## Context
Several people edit, approve and settle the same bills. Clients can be offline or stale.

## Decision
The same split and settlement algorithms in `core/domain` run on client and server, but only server results are final. Every mutating request carries an idempotency key.

## Consequences
Clients show previews and reconcile with the server response. The server needs transactions and constraints, hence PostgreSQL.
