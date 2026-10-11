# 0014. Role permissions

Status: Accepted
Date: 2026-10-11

## Context
The plan defines three roles (owner, member, cook) and says every endpoint checks membership and role, but not what
each role may do.

## Decision
`core:domain` `RolePolicy` is the single source, used by the app (to hide actions) and the server (to enforce them):
- **Owner:** everything.
- **Member:** everything except inviting people and closing a month early.
- **Cook:** view the household, view the pantry and update it (mark items finished, move them). Cooks never see
  bills, balances or settlements.
Only owners can create invites, and only owners can invite another owner.

## Consequences
Phase 1 has no role editing; changing a role means a new invite. Revisit if flats want members to invite.
