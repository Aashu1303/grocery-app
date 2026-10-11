# Milestones

Each milestone closes on a commit whose CI run is fully green (privacy guard, JVM/Compose/Android, iOS).
Tags are annotated `mN-complete`. If a tag is missing on GitHub, create it from this table:
`git tag -a m0-complete <commit> -m "M0" && git push origin m0-complete`.

| Milestone | Commit | Green CI run | Notes |
| --- | --- | --- | --- |
| M0 Foundation | `79ca6ab` | [run 13](https://github.com/Aashu1303/grocery-app/actions/runs/38096739005) | Includes the first M1 domain and server commits, which were pushed before M0's final CI fix. |
| M1 Household and auth | `434437f` | [run 23](https://github.com/Aashu1303/grocery-app/actions/runs/38099561232) | Server auth, households, invites, roles, locations; sign-in, setup and household screens; demo step 1. |
| M2 Invoice import | `844e624` | [run 34](https://github.com/Aashu1303/grocery-app/actions/runs/38109498311) | Four-platform parser with reconciliation, LLM fallback, bills and draft editing, import and review screens, demo steps 2 and 7. |
| M3 Pantry | `d8bc4cb` | [run 42](https://github.com/Aashu1303/grocery-app/actions/runs/38110429779) | Categorizer and expiry rules, pantry created from food lines, pantry API and screen, item food toggle, demo step 5. The commit also carries the M4 split domain. |
| M4 Bill split and approvals | `f06e8c0` | [run 48](https://github.com/Aashu1303/grocery-app/actions/runs/38112309997) | Line splits (one person, equal, quantity), shared fees, hand-typed bills, submit and approve with 48 h auto-approve, bills list and detail screens, demo steps 3 and 4. |
| M5 Ledger and settlement | `f9c0061` | [run 80](https://github.com/Aashu1303/grocery-app/actions/runs/38126166954) | Periods, balances, simplified transfers, month close, UPI link, settlements, ledger screens, demo step 8. Closed together with M6 and M7 on one head; see the note below the table. |
| M6 Kitchen | `f9c0061` | [run 80](https://github.com/Aashu1303/grocery-app/actions/runs/38126166954) | Use-first ranking, expiry alerts through a notifier (FCM or no-op), dish suggestions limited to in-stock items, kitchen screens, demo step 6. |
| M7 Hardening | `f9c0061` | [run 80](https://github.com/Aashu1303/grocery-app/actions/runs/38126166954) | Demo step 9 on every endpoint, 200% text screenshots, Android release build (HTTPS only, Firebase sign-in), iOS Firebase bridge, server Dockerfile. |

M5 to M7 were validated on the self-hosted macOS runner (ADR 0025) because GitHub-hosted minutes ran out. That run
passed the guards, the JVM, Compose desktop and Android debug and release builds, and the iOS tests and app build,
but it skips `verifyRoborazziJvm`. Goldens for the kitchen screens, the `_large_text` captures and the ledger and
bills surface fix still have to be recorded on a hosted Linux runner (a `[record screenshots]` commit) and verified
there once minutes are available.
