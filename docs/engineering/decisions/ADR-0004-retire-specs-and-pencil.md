---
id: ADR-0004
title: Retire specs and Pencil fidelity loop
status: Active
date: 2026-07-06
---

# ADR-0004: Retire specs and Pencil fidelity loop

## Decision

- Retire `specs/`, including shadow specs and `specs/DESIGN_SYSTEM.pen`.
- Retire the Pencil fidelity loop and the `/spec` to shadow-spec planning flow.
- Treat `docs/engineering/` plus source code as the single source of truth.
- Treat the design system as source code under `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/` plus `docs/engineering/design-system.html`.

## Why

- `specs/` became stale and unreliable compared with as-built engineering docs and code.
- `specs/DESIGN_SYSTEM.pen` depends on an external Pencil app workflow that is flaky enough to block normal maintenance.
- Existing `docs/engineering/` ADRs, conventions, traps, and architecture docs already record current truth from code.
- Keeping two planning/spec systems creates drift risk without runtime benefit.

## Affects

Docs:

- `docs/engineering/`
- `docs/engineering/design-system.html`
- `docs/engineering/decisions/README.md`
- `CLAUDE.md`
- `AGENTS.md`

Retired paths:

- `specs/*`
- `specs/shadow/*`
- `specs/DESIGN_SYSTEM.pen`
- `.claude/skills/agentic-engineering/`

## Consequences

- Good: contributors use one live documentation system: `docs/engineering/` plus code.
- Good: no Pencil dependency for design-system maintenance.
- Good: no `/spec` to shadow-spec flow to keep synchronized.
- Cost: unbuilt screen intent that only existed in `DESIGN_SYSTEM.pen` is dropped.
- Risk: future design changes must update code and `docs/engineering/design-system.html` together to avoid drift.

## Read when

- changing repo planning workflow or agent instructions
- adding or changing design-system tokens/components
- deciding whether to recreate specs, shadow specs, or Pencil assets
- editing `docs/engineering/` source-of-truth policy

## Supersedes

- `specs/` as a contributor source of truth.
- The `/spec` to shadow-spec workflow.
- The Pencil fidelity loop based on `specs/DESIGN_SYSTEM.pen`.
