# 0018. Invoice upload as JSON, bill storage and draft editing

Status: Accepted
Date: 2026-10-11

## Context
Design-plan 6.6 lists `POST /households/{id}/invoices` ("upload one or more PDFs") and `PATCH /bills/{id}`, and 6.2
describes the bill tables. Building the import showed some gaps:

- Every write must be idempotent (ADR 0015). The idempotency layer hashes and replays JSON bodies.
- The review screen must be able to reload a draft.
- The QUANTITY split needs a count, while weights and pack sizes are separate facts.
- Uploaded PDFs must be kept somewhere.

## Decision
- **Upload body.** The upload is JSON: `{"documents":[{"fileName","contentBase64"}]}`. It goes through the same
  idempotency path as every other write. Bodies are read with a byte limit: 64 KiB by default, 15 MiB for uploads
  (up to five PDFs). File names are not stored, because they often contain order numbers.
- **Multi-PDF bills.** The documents must come from one platform and one order (same order id), and the merged bill
  keeps the first invoice number. Duplicates are rejected with `409 duplicate_invoice` in two ways:
  - The same file (SHA-256) or the same platform invoice number was already imported into the household.
  - A unique constraint on `bill_documents` catches concurrent uploads.
- **Unreadable documents.** These are reported by position (`documents[1]: not_a_pdf`, `unsupported_format`, ...).
  When the LLM fallback itself is down, the document is reported as `unsupported_format`, so the user can still
  add the bill by hand.
- **Storage.** PDFs are stored through a `DocumentStorage` interface under `households/<id>/<sha256>.pdf`. The
  server uses the filesystem, at `RASOI_STORAGE_DIR` (default `data/documents`). A failed database insert removes
  the files it just wrote.
- **Draft visibility and editing.**
  - A new bill is a `DRAFT` with the uploader as payer, visible only to the uploader until it is submitted (M4).
  - `GET /bills/{id}` is added so the review screen can reload a draft.
  - In M2, `PATCH /bills/{id}` replaces a draft's lines, charges or printed total. Line ids are kept, so later
    allocations stay attached.
  - Review warnings (total mismatch, read by the fallback) are derived on every read, never stored.
- **Bill line columns.** `bill_lines` stores:
  - `units`: the count used by QUANTITY splits.
  - An optional `measure_amount` and `measure_unit`: a weight or pack size.
  - `category` and `is_food`: nullable until the M3 categorizer fills them.
  `bills.period_id` arrives with periods in M5.
- **LLM fallback configuration.** The fallback is enabled only when `RASOI_LLM_BASE_URL`, `RASOI_LLM_API_KEY` and
  `RASOI_LLM_MODEL` are all set.

## Consequences
- Base64 adds about a third to upload size, which is acceptable for invoices of a few hundred kilobytes, and keeps
  the client simple on both platforms.
- Server and demo tests upload PDFs rendered from the redacted fixtures (`FixturePdf`, a `server:parsing` test
  fixture), so no real invoice is ever needed.
