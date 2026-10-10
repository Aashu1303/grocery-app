## Claude Code kickoff prompt

You are building Phase 1 of a shared-flat grocery app: PDF invoice import, a live pantry,
expiry tracking, dish suggestions from stock, and fair bill splitting with month-end settlement.
The full spec is docs/design-plan.md. Read it completely before doing anything. It is the
source of truth; if you must deviate, update it and add an ADR in docs/adr/.

Working rules:
- Ask questions only when absolutely necessary. Work autonomously, make reasonable
  decisions, and record them as ADRs.
- IMPORTANT: Do NOT look outside this working directory or use any other code on this
  computer as a reference.
- Start by asking me one question to choose the app name. Suggest 4 to 5 options that suit
  an Indian shared-flat grocery and kitchen app. Then do not ask anything else unless blocked.
- Stack is fixed by the plan: Kotlin Multiplatform with Compose Multiplatform (Android and iOS),
  Ktor server, PostgreSQL, SQLDelight, Koin. Follow its module layout exactly.
- This is production-grade, not a hack project: clean architecture, MVI, money as Long paise,
  typed errors, no shortcuts, no TODO-driven code left behind.

Testing is of the utmost importance:
- Write lots of unit tests (kotlin.test; Turbine for flows). Domain and parsers are test-first.
- Run the full automated test suite after every meaningful change and before every commit.
- Build the end-to-end demo scenario from section 8 of the plan: a seeded demo household that
  exercises every feature and every API endpoint. It must pass before a milestone closes.
- Build parser golden tests from the invoices in fixtures/invoices/: create redacted text
  fixtures in fixtures/invoices/ (replace names, addresses, phone numbers, order and invoice
  numbers). Never commit anything from fixtures/invoices/; make sure it is git-ignored.
- Build the design-system catalog screen and screenshot tests for every component and state.

Design system:
- Implement core/designsystem first (tokens, AppTheme, components from section 7).
- Features use only design-system tokens and components. No raw colours, sizes or strings.

Agent support:
- Create AGENTS.md with concrete development guidelines: module map, build and test commands,
  architecture rules, code style, definition of done.
- Create skills in .claude/skills/: new-feature-module, add-invoice-parser,
  add-design-component, write-adr, pre-commit-review.

Code style:
- Official Kotlin coding conventions plus the Android Kotlin style guide for Compose, with the
  naming and suffix rules in section 9. Enforce with ktlint and detekt in CI.
- Compact code; no comments that restate code. KDoc only for public domain APIs and
  non-obvious algorithms.

Git:
- Initialise a git repository, set up GitHub Actions CI (build, lint, test), and push to
  https://github.com/Aashu1303/grocery-app.git.
- Work milestone by milestone (M0 to M7 in section 1). Use Conventional Commits and tag each
  completed milestone.
- IMPORTANT: Before every commit, do a code review using the checklist in section 9. Every
  change must comply with the code style and production-grade standards, and have unit tests
  that pass. Fix every issue before committing.

Start with M0. After each milestone, give me a short summary: what was built, test results,
and any decisions recorded.