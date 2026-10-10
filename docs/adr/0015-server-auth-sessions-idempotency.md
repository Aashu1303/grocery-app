# 0015. Server sign-in, sessions, idempotency and wiring

Status: Accepted
Date: 2026-10-11

## Context
The plan fixes Firebase phone auth with server-side token verification, idempotent writes, membership checks on
every endpoint, and a data model without a sessions table. Local development, CI and the end-to-end demo scenario
must run without a Firebase project.

## Decision
- **ID tokens:** `FirebaseIdTokenVerifier` checks RS256 signatures against Google's published keys (cached JWKS),
  issuer `https://securetoken.google.com/<FIREBASE_PROJECT_ID>`, audience, expiry (60 s leeway), subject and
  `phone_number`. No Firebase Admin SDK is needed.
- **Dev sign-in:** with `RASOI_AUTH_MODE=dev` the server accepts `dev:<phone>` tokens (`DevIdTokenVerifier`) and logs a
  warning at startup. Firebase mode is the default and requires `FIREBASE_PROJECT_ID`.
- **Sessions:** `POST /auth/session` issues an opaque 256-bit bearer token. Only its SHA-256 hash is stored in a new
  `sessions` table, with a 30-day expiry (`SESSION_TTL_DAYS`). Tokens are revocable by deleting rows.
- **Idempotency:** every mutating endpoint except `/auth/session` requires `Idempotency-Key`. The first response
  (status and JSON body) is stored per (user, key) in `idempotency_keys`; a retry with the same body replays it, and
  the same key with a different body is rejected with 409 `idempotency_key_reused`.
- **Access:** non-members get 404 for every household resource (existence is not revealed); members without the
  permission (ADR 0014) get 403.
- **Endpoints added to the plan:** `GET /me` (the caller and their households) and `GET /households/{id}` (members
  and locations), which the app needs after sign-in.
- **Wiring:** the server uses a plain composition root (`AppGraph`) instead of Koin; it has few objects, and
  constructor wiring keeps tests explicit. Koin remains the app's DI.
- **Persistence:** Exposed 1.x DSL and Flyway migrations in `server/app` (`persistence` package); tests use
  Testcontainers PostgreSQL 17.

## Consequences
The data model gains `sessions` and `idempotency_keys` tables. Expired sessions and old idempotency keys need
periodic cleanup, which the jobs module will schedule (M4). A real Firebase project is needed only for production
sign-in.
