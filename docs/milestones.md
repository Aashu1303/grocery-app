# Milestones

Each milestone closes on a commit whose CI run is fully green (privacy guard, JVM/Compose/Android, iOS).
Tags are annotated `mN-complete`. If a tag is missing on GitHub, create it from this table:
`git tag -a m0-complete <commit> -m "M0" && git push origin m0-complete`.

| Milestone | Commit | Green CI run | Notes |
| --- | --- | --- | --- |
| M0 Foundation | `79ca6ab` | [run 13](https://github.com/Aashu1303/grocery-app/actions/runs/38096739005) | Includes the first M1 domain and server commits, which were pushed before M0's final CI fix. |
