# 0029. Screen padding and the top bar

Status: Accepted
Date: 2026-10-15

## Context

The plan set "screen padding 16" (section 7.1) but not where it applies, and screens drifted. Sign-in used 24 dp,
the Pantry list 12 dp vertically, and the Material `TopAppBar` fixed its height at 64 dp, so a two-line title (a long
household name) touched the status bar on a real phone. The owner asked for 16 dp horizontal and vertical padding
everywhere, in the app and in the Figma designs.

## Decision

- **One rule:** every screen pads its content by `spacing.screenPadding` (16 dp) horizontally and vertically, measured
  from the status bar, the top bar or the bottom bar. Screens without a top bar (sign-in, household setup) use the
  same 16 dp; there is no extra top gap.
- **`AppTopBar`** is a design-system row instead of Material's `TopAppBar`: 16 dp above, below and before the title,
  at least 64 dp tall, growing for a two-line title. Back and action icon buttons keep their 48 dp touch targets
  with a 4 dp inset, so their 24 dp icons sit on the same 16 dp edge.
- **Figma** mirrors it: the AppTopBar component and the screen frames use 16 dp padding.

## Consequences

- Screens with a top bar and a one-line title look as before; two-line titles get room. Screenshot goldens for
  every screen with a top bar change once.
- A new screen gets the rule by using `screenPadding`; the plan's UI rules list it (7.3, rule 11).
- An avatar action is 40 dp inside a 48 dp target, so it sits about 8 dp from the edge rather than 16 dp; revisit if
  that looks off next to icon actions.
