# Architecture Decision Records

One file per decision: `NNNN-short-title.md`, numbered in order and never renumbered.
Copy `0000-template.md`. A superseded ADR stays in place with its status changed.

| ADR | Decision | Status |
| --- | --- | --- |
| [0001](0001-server-side-pdf-parsing.md) | Parse invoice PDFs on the server | Accepted |
| [0002](0002-money-as-paise.md) | Money as `Long` paise, quantities as `Long` base units | Accepted |
| [0003](0003-server-authoritative-ledger.md) | The server is the source of truth for bills and the ledger | Accepted |
| [0004](0004-firebase-auth-and-push.md) | Firebase for phone auth and push | Accepted |
| [0005](0005-offline-reads-online-writes.md) | Offline reads, online-only writes in Phase 1 | Accepted |
| [0006](0006-app-name-rasoi.md) | App name Rasoi, package `app.rasoi` | Accepted |
| [0007](0007-jvm-targets-and-ci-verified-mobile.md) | JVM and desktop targets for local verification; mobile verified in CI | Accepted |
| [0008](0008-single-designated-branch.md) | Work on the designated branch, tag milestones | Accepted |
| [0009](0009-invoice-fixtures-and-pii.md) | Real invoices are never committed; redacted text fixtures only | Accepted |
