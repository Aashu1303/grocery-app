---
name: write-adr
description: Record an architecture decision in docs/adr and update the design plan when the decision deviates from it. Use whenever a choice affects structure, dependencies, data, security or workflow.
---

# Write an ADR

1. Pick the next number: `ls docs/adr | sort | tail -1`. Never renumber existing ADRs.
2. Copy `docs/adr/0000-template.md` to `docs/adr/NNNN-kebab-title.md`.
3. Fill it in briefly:
   - **Status:** Accepted (or Proposed if waiting on the owner). **Date:** today.
   - **Context:** the forces and constraints, with facts (versions, measurements, plan sections).
   - **Decision:** what we do, concretely (modules, files, commands).
   - **Consequences:** what gets easier, what gets harder, and when to revisit.
4. Add a row to the table in `docs/adr/README.md`.
5. If the decision changes anything in `docs/design-plan.md`, edit the plan in the same commit and reference the
   ADR number next to the change.
6. If it supersedes an older ADR, set the old one's status to `Superseded by NNNN`.
7. Commit as `docs(adr): NNNN <title>` (or together with the change it explains).
