#!/usr/bin/env bash
# Fails if anything under fixtures/invoices other than redacted fixtures, golden snapshots or the README is tracked
# (ADR 0009).
set -euo pipefail
offending=$(git ls-files fixtures/invoices | grep -vE '(^fixtures/invoices/README\.md$|\.redacted\.txt$|\.expected\.json$)' || true)
if [[ -n "$offending" ]]; then
  echo "Real invoice files must never be committed:" >&2
  echo "$offending" >&2
  exit 1
fi
echo "Invoice fixture guard passed."
