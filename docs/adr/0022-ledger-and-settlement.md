# 0022. Ledger, month close and settlement

Status: Accepted
Date: 2026-10-11

## Context
M5 turns approved bills into balances and transfers (design-plan 6.3 and 6.4 items 4 and 5). The plan fixes the
algorithms; it leaves open when a bill joins a period, how a month closes without losing a concurrent change, who may
mark a transfer paid, and how a UPI payment is started.

## Decision
- **Periods.** A period is a month (`2026-10`) in the household's time zone. Every bill joins the household's open
  period when it is created; a household has at most one open period, opened lazily for the current month. A database
  constraint enforces "one open period".
- **Balances** come from approved and locked bills only: the payer is credited the total and each member debited their
  share (ADR 0021). They always sum to zero. Cooks never appear. Bills still waiting for approval are counted and
  reported, not included.
- **Transfers** are the largest-creditor-with-largest-debtor matching, at most n - 1 for n members, ties broken by
  member id. In an open period they are suggestions; closing stores them as settlements.
- **Closing** happens when the month has ended (a sweep every 15 minutes, like auto-approval) or when an owner closes
  the open period early. One transaction locks the period, locks its approved bills, moves every other bill to the next
  period, opens that period and stores the settlements. It takes row locks on the bills and bumps their versions, so a
  save based on an older read fails with `bill_changed` instead of undoing the lock. The next period is the month
  after the closed one, or today's month if that is later (a late close).
- **Marking paid.** The person who pays, the person who is paid, or an owner can mark a stored transfer paid; doing it
  twice is harmless. Nothing moves money: Rasoi records what people say they did.
- **UPI.** The payee's UPI address (set with `PATCH /me`, validated as `name@bank`) comes with each transfer. The app
  builds a `upi://pay` link (payee, amount in rupees, note "Rasoi 2026-10") and hands it to the platform, which opens the
  user's UPI app. Opening a link is the same kind of hand-off as sharing, so it is the one new `expect`/`actual`
  (`UpiLauncher`) beside file picking, sharing, notifications and secure storage (AGENTS.md rule 7).
- **Migration** `V5` backfills one open period for households that already had bills.

## Consequences
- A month can be closed once; asking again is `409 period_closed` or `period_not_open`, and the app reloads.
- Bills added after an early close land in the next month, never in a locked one.
- The ledger is derived from bills on every read, so it cannot drift; stored settlements are the only extra state.
