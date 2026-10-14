# 0028. Hosting: Cloud Run, Neon Postgres and scheduler-driven sweeps

Status: Accepted
Date: 2026-10-15

## Context

Design-plan section 4 left hosting open. The owner's constraints: stay inside the Firebase project
(`grocery-app-2c112`) where possible, cost at most about $2 a month, and expect under 10,000 API calls a month for
now. The server is one Ktor container (ADR 0024) that needs PostgreSQL, a persistent folder for invoice PDFs, HTTPS
and its three 15-minute sweeps (ADR 0021).

- **Cloud Run** has a free tier far above that load when it scales to zero and bills per request, but between
  requests it throttles the CPU to almost nothing, so the in-process `JobRunner` timer would stall. Keeping CPU always
  on costs about $45–55 a month.
- **Cloud SQL** has no free tier: the smallest instance is about $11 a month plus storage in Singapore. The Firebase
  Data Connect trial is free for 90 days but cannot be used as a plain database by our server.
- **Neon** (serverless Postgres) has a free plan (0.5 GB, 100 compute-hours a month) that suspends when idle. It has
  no India region; Singapore is the nearest.

## Decision

- **Server:** Cloud Run service `rasoi-api` in `asia-southeast1` (Singapore, next to the database), request-based
  billing, 0 to 1 instances (the server is tested single-process), 1 vCPU and 1 GiB with startup CPU boost.
- **Database:** Neon Postgres 17 in AWS `ap-southeast-1`, direct (unpooled) connection over TLS through the existing
  JDBC driver (`sslmode=require`); Flyway migrates on start as before. No new dependency.
- **Invoice PDFs:** a private Cloud Storage bucket mounted at `/data` (Cloud Storage FUSE, gen2), so
  `FileDocumentStorage` is unchanged. Its temp-file-then-rename write becomes copy-then-delete on a flat bucket,
  which is still never a half-written PDF to readers. The S3-compatible storage in the plan stays unbuilt.
- **Sweeps:** a new `RASOI_SWEEP_TOKEN` setting. When set, the server registers `POST /internal/sweeps` (checked in
  constant time against the `X-Rasoi-Sweep-Token` header, otherwise 401) and does not start the timer; Cloud Scheduler
  calls it every 15 minutes. Without the setting nothing changes: the endpoint does not exist and the timer runs.
  Sweeps only do what is due, so a late, missed or retried call is safe; a failing sweep answers 500 so the scheduler
  shows and retries it.
- **Secrets** (database password, sweep token) live in Secret Manager and reach the service as environment variables.
  `scripts/deploy-cloud-run.sh` creates everything idempotently and deploys the current commit.

## Consequences

- Expected cost is the free tiers plus cents for storage. The real variable cost is Firebase phone sign-in: about
  $0.07 per SMS in India after 10 a day; test phone numbers in the Firebase console send none.
- After about 15 idle minutes the next request waits for a JVM cold start (several seconds) and Neon may add a short
  resume. The sweep calls usually keep an instance warm.
- India to Singapore adds roughly 60 ms per request.
- The sweep token sits in the scheduler job's headers, readable by project members who can view the job.
- Moving to Cloud SQL later is a `pg_dump` and a new `DATABASE_URL`; nothing in the code is Neon-specific.
- Revisit when the free limits come close (0.5 GB of data, 100 compute-hours) or a second instance is needed.
