---
id: ADR-0004
title: Retire legacy specs and external design loop
status: Active
date: 2026-07-06
---

# ADR-0004: Retire legacy specs and external design loop

## Decision

- Retire the legacy spec tree, including shadow specs and the retired design capture.
- Retire the external design-app fidelity loop and command-driven shadow-spec planning flow.
- Treat `docs/engineering/` plus source code as the single source of truth.
- Treat the design system as source code under `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/` plus `docs/engineering/design-system.html`.

## Why

- The legacy spec tree became stale and unreliable compared with as-built engineering docs and code.
- The retired design capture depended on an external app workflow that was flaky enough to block normal maintenance.
- Existing `docs/engineering/` ADRs, conventions, traps, and architecture docs already record current truth from code.
- Keeping two planning/spec systems creates drift risk without runtime benefit.

## Affects

Docs:

- `docs/engineering/`
- `docs/engineering/design-system.html`
- `docs/engineering/decisions/README.md`
- `CLAUDE.md`
- `AGENTS.md`

Retired areas:

- legacy spec tree
- shadow-spec files
- retired design capture
- retired shadow-spec planning skill

## Consequences

- Good: contributors use one live documentation system: `docs/engineering/` plus code.
- Good: no external design-app dependency for design-system maintenance.
- Good: no command-driven shadow-spec flow to keep synchronized.
- Cost: unbuilt screen intent that only existed in the retired design capture is dropped.
- Risk: future design changes must update code and `docs/engineering/design-system.html` together to avoid drift.

## Read when

- changing repo planning workflow or agent instructions
- adding or changing design-system tokens/components
- deciding whether to recreate legacy specs, shadow specs, or external design assets
- editing `docs/engineering/` source-of-truth policy

## Supersedes

- The legacy spec tree as a contributor source of truth.
- The command-driven shadow-spec workflow.
- The external design-app fidelity loop.
