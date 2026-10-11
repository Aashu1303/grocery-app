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
| [0007](0007-jvm-targets-and-ci-verified-mobile.md) | JVM targets for local verification; mobile and Compose verified in CI | Accepted |
| [0008](0008-single-designated-branch.md) | Work on the designated branch, tag milestones | Accepted |
| [0009](0009-invoice-fixtures-and-pii.md) | Real invoices are never committed; redacted text fixtures only | Accepted |
| [0010](0010-compiled-convention-plugins.md) | Convention plugins are compiled Kotlin classes | Accepted |
| [0011](0011-code-style-tooling.md) | Code style tooling: ktlint, detekt, source and module rules | Accepted |
| [0012](0012-xcodegen-ios-wrapper.md) | iOS wrapper generated with XcodeGen | Accepted |
| [0013](0013-agp9-android-app-module.md) | AGP 9, compileSdk 37 and a thin androidApp module | Accepted |
| [0014](0014-role-permissions.md) | Role permissions | Accepted |
| [0015](0015-server-auth-sessions-idempotency.md) | Server sign-in, sessions, idempotency and wiring | Accepted |
| [0016](0016-client-sign-in-providers.md) | Client phone sign-in providers | Accepted |
| [0017](0017-invoice-parsing-design.md) | Invoice parsing: positioned-text fixtures, structural detection, per-platform split basis | Accepted |
| [0018](0018-invoice-upload-and-bill-storage.md) | Invoice upload as JSON, bill storage and draft editing | Accepted |
| [0019](0019-import-review-flow.md) | Invoice import and review flow on the client | Accepted |
| [0020](0020-pantry-from-bills.md) | Pantry from bill lines, categories and expiry estimates | Accepted |
| [0021](0021-splits-approvals-and-scheduled-work.md) | Splits, approvals and scheduled work | Accepted |
| [0022](0022-ledger-and-settlement.md) | Ledger, month close and settlement | Accepted |
