# 0023. Kitchen: use-first, expiry alerts, push and dish ideas

Status: Accepted
Date: 2026-10-11

## Context
M6 adds the kitchen home screen (design-plan 6.4 items 7 and 8, API rows for `kitchen/use-first`,
`kitchen/suggestions` and `/devices`) and the push channel the plan names for approvals and expiry alerts. The plan
leaves open how pushes are sent without a Firebase project, how alerts avoid repeating, and what dish suggestions may
send to the language model.

## Decision
- **Use-first** is a pure domain function over in-stock food items: soonest expiry first, then the smaller quantity,
  then name; items with no expiry come last. The server returns it from the pantry on every read.
- **Notifier.** `server:notifications` holds a `Notifier` interface (`send(tokens, message)` returning the tokens the
  provider rejected), an FCM HTTP v1 implementation and a `NoopNotifier`. FCM is used only when
  `FCM_SERVICE_ACCOUNT_FILE` points at a service-account JSON file; otherwise nothing is sent. The OAuth token comes
  from a signed JWT exchanged at Google's token endpoint (java-jwt, already a dependency). Rejected tokens are
  deleted. A failed push is logged without personal data and never fails the request that caused it.
- **Who is told.** Approvers when a bill is submitted, the uploader on a change request, all members when food is about
  to expire or has expired. Messages carry item names or a bill title, never amounts or phone numbers.
- **Expiry alerts** are a sweep registered in `scheduleJobs` beside auto-approval and month close (every 15 minutes, ADR
  0021). Items within the "use soon" window or past their date are alerted once per kind; `expiry_alerts_sent`
  (`V6`) remembers `(item, kind)` so repeated runs send nothing twice. No jobs table.
- **Devices.** `POST /devices` upserts a token for the signed-in user (idempotent, token is the key, so a phone that
  changes accounts moves with it).
- **Dish ideas** go through the same `ChatCompletionClient` as the invoice fallback. Only the names of in-stock items
  are sent, never household, member, price or bill data. The reply must be JSON; dishes whose ingredients are not all
  in the sent list are dropped, so the app never suggests something the flat cannot cook. With no chat model
  configured the endpoint answers `503 unavailable` and the app says suggestions are off. Cooks may use both read
  endpoints (they hold VIEW\_PANTRY).

## Consequences
- Running a flat without Firebase or an LLM still works; pushes and ideas are simply absent.
- The sweep reads the pantry over a three-day window in UTC and then applies each household's own time zone, so no
  household is missed or alerted early.
- Dish ideas cost one model call per tap; there is no caching or background generation.
