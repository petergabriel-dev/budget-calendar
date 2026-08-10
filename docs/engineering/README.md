# Engineering Docs

These docs are plain Markdown for all contributors.

This project supports Pi documentation tooling for initialization,
validation, and generated indexes. If you edit generated sections,
run the docs tooling to refresh them.

## Structure

- **architecture.md** — System shape, component boundaries, data flow
- **dev-workflow.md** — Setup, env, commands, build/test/deploy steps
- **conventions.md** — Naming, style, patterns, coding rules
- **invariants.md** — Must-not-break rules
- **traps.md** — Known gotchas, pitfalls, and issues
- **design-system.html** — Offline design-system token + component catalog
- **decisions/** — Architectural decision records (ADRs)

## Shipped Compose design handoff

- `docs/design/` records implemented Home, Calendar, Accounts, Me/Settings, Sandbox, and shared-sheet compositions.
- Runtime source is shared Compose in `commonMain`; `BudgetCalendarTheme` follows system light/dark appearance; App owns four tabs and global notification state.
- Verify from `src/mobile/` with `:composeApp:allTests`, `:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug`, and `:composeApp:connectedDebugAndroidTest`.
