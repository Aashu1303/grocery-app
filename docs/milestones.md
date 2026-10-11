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
