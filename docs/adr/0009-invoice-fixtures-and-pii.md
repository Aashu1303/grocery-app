# 0009. Real invoices are never committed; redacted text fixtures only

Status: Accepted
Date: 2026-10-10

## Context
Invoices contain names, addresses, phone numbers and order numbers. Real PDFs were committed in the first import before this rule was in place.

## Decision
`fixtures/invoices/` is git-ignored except `*.redacted.txt` files and the folder README. Parser golden tests read only the redacted text. CI fails if any PDF under `fixtures/invoices/` is tracked. The PDFs were untracked in M0.

## Consequences
The earlier commit still holds the PDFs in remote history. Removing them needs a history rewrite and force-push, which is the owner's decision.
