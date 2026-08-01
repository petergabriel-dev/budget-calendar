---
id: ADR-0011
title: Fixed six-row calendar grid
status: Active
date: 2026-08-01
---

# ADR-0011: Fixed six-row calendar grid

## Decision

- Calendar month construction always returns 42 contiguous dates, starting Sunday on or before the first day of target month.
- Calendar renders six weighted rows from leftover vertical space; cells are weighted and fill their row height.
- Calendar has no lazy layout, scroll state, or hardcoded day-cell height.

## Why

- Six rows keep `DayTransactionSectionHeader` and transaction list stable across 4-, 5-, and 6-week month spans.
- Leftover-space sizing removes parent/child height-constant drift, so grid cannot overflow its assigned space.
- Plain static layout removes lazy-grid state restoration that could preserve hidden scroll position across month changes.

## Affects

Docs:

- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/BuildCalendarMonthUseCase.kt:48-65`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarGrid.kt:45-60`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt:66-76`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:111-142`
- `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/BuildCalendarMonthUseCaseTest.kt:18-57`

## Consequences

- Good: month navigation does not shift calendar or transaction-list layout.
- Good: no duplicate size constants or grid scroll range can clip rows.
- Cost: 4- and 5-week months show an extra greyed week.
- Cost: day-cell height varies by device; measured values must meet touch-target requirements.
- Constraint: `verticalScroll` cannot be fallback because weighted rows require bounded height.

## Read when

- changing calendar month construction, grid layout, or day-cell sizing
- adding calendar scrolling, lazy layout, or variable row counts
- changing calendar screen vertical allocation or transaction-list height

## Supersedes

- Week-boundary-only calendar padding and fixed-height lazy calendar grid.
