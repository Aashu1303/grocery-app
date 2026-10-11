# 0021. Splits, approvals and scheduled work

Status: Accepted
Date: 2026-10-11

## Context
M4 adds the money rules (design-plan 6.3 and 6.4): who owes what on a bill, how the flat agrees to it, and what
happens when nobody answers. The plan fixes the algorithms and the lifecycle; it leaves open how derived amounts are
stored, who may edit a submitted bill, how scheduled work survives a restart, and how a bill that has no invoice is
entered.

## Decision
- **Allocations are stored, charge shares are derived.** Each line's split (`ONE`, `EQUAL`, `QUANTITY`) is computed
  with largest-remainder rounding and stored as one `allocations` row per member and line, so it always sums to the
  line. A bill's fees and discounts are shared on demand by `BillShares` (proportionally to each member's lines, or
  equally per charge), so editing a charge never leaves stale rows. The API returns both allocations and per-member
  shares.
- **A line's split follows its amount.** When an edit changes a line's amount the existing split is recomputed. If
  the number of units no longer fits a quantity split, the split is dropped; the bill must be split again before it
  can be submitted, and a submitted bill rejects such an edit.
- **Who owes.** Only members whose role may approve bills (not cooks) can hold a share or be the payer. Required
  approvers are the members with a non-zero share, except the payer when the payer is also the uploader.
- **Who may edit.** Anyone with bill-edit rights may edit their own draft. A submitted bill may be edited by its
  uploader or an owner, until the month locks. An edit asks again only the members whose total changed or who became
  required (changing the payer can do that); everyone else keeps their approval.
- **Submitting** needs every line split and no total mismatch. A bill with a change request can be submitted again;
  approvals already given stay.
- **No jobs table.** The plan listed a durable `jobs` table. The scheduled work is all derived from stored state:
  a pending bill is due when `submitted_at` is 48 hours old, an item is due for an alert when its expiry nears, a
  month is due when the household's clock passes its first day. A sweep that runs every few minutes finds what is due
  and is safe to run twice, so a restart loses nothing and there is no queue to drift out of sync with the data.
  `server:jobs` holds only a generic `JobRunner` (fixed delay, failures reported and survived, stopped with the
  application scope); each milestone registers its sweep in `scheduleJobs`. The plan's table row is removed.
- **Silence approves after 48 hours**, marked `automatic` so the app can say so. The sweep runs every 15 minutes, so
  approval can come up to 15 minutes late.
- **Concurrent changes.** Saving a bill rewrites its lines, allocations and approvals as one unit, so two people deciding
  at once could overwrite each other. Each bill carries a `version`; a save succeeds only if the version it read is
  still current, otherwise nothing is written and the request fails with `409 bill_changed`. The app reloads and
  retries.
- **Manual bills** (`POST /households/{id}/bills`) are drafts on the `MANUAL` platform with a title, lines and charges
  typed in. The server sets the total to the lines plus charges, so they never carry a mismatch. Each gets a unique
  invoice number derived from its id.

## Client
- `feature:bills` has two screens. The **list** shows every bill with its status and offers "Import invoice" and "Add a
  bill"; the second takes a title and a total, creates a one-line manual bill and opens it. The **detail** screen shows
  the status, the items with who shares each, what each person owes, and the next step for the viewer: split items,
  send for approval, approve, or ask for changes with a reason.
- Splitting works one item at a time in a sheet (one person, equal, or by quantity), with the same allocator the
  server uses so mistakes show before a request is sent. Cooks are never offered as sharers.
- Editing the lines of a draft stays in the invoice review screen (ADR 0019); the detail screen links to it.
- Every change is saved to the server straight away. If the server answers `bill_changed`, the screen reloads the bill
  and says so; the user repeats the change.

## Consequences
- Charge edits and payer changes need no data repair.
- Auto-approval is testable by moving the test clock and calling the sweep; the demo scenario does exactly that.
- A cook can still see and edit the pantry but never sees bills, so a bill listing returns 403 for them.
