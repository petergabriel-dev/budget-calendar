---
id: ADR-0015
title: Calendar tap opens day sheet
status: Active
date: 2026-10-02
---

# ADR-0015: Calendar tap opens day sheet

## Decision

- Calendar has no persistent selected date or inline day-transaction section. Tapping a date opens its day sheet.
- Tapping an adjacent-month date displays its containing month before opening the sheet. Home `View all` opens the sheet for the exact handed-off date.
- Sheet rows come from the displayed month's `CalendarUiState.transactionsOn(date)`. Opening details or edit hides the day sheet without clearing its date; dismiss, cancel, save, and delete return to the day sheet with current rows.

## Why

- Selecting a date and then tapping a separate expand control required two actions to view the day's transactions.
- The displayed month's transaction map already groups rows by local calendar date and excludes cancelled transactions; reusing it avoids a second row-fetch path.
- Switching months before opening an adjacent date keeps sheet rows inside the displayed month. In-month opens need no month reload, and the separate off-month selection flow is unnecessary.

## Affects

Docs:

- `docs/engineering/architecture.md`
- `docs/engineering/conventions.md`
- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/ADR-0011-fixed-six-row-calendar-grid.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:224-245`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:50-71`, `:138-212`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarViewModel.kt:92-95`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarUiState.kt:12-24`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/BuildCalendarMonthUseCase.kt:28-67`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt:37-120`

## Consequences

- Good: one date tap opens the full transaction list; in-month opens reuse loaded month state.
- Good: transaction changes continue through the existing month flow and update an open day sheet.
- Constraint: the sheet date always belongs to the displayed month; adjacent dates switch the month first, so an open sheet cannot peek into another month.
- Cost: modal sheet dismissal on configuration change remains consistent with other `remember`-based Calendar sheets.

## Read when

- changing calendar date taps, day-sheet rows, or sheet dismissal behavior
- changing Home-to-Calendar date handoff or month loading
- changing transaction detail, edit, or delete return flows

## Supersedes

- The inline day panel and month-independent selected-day behavior from the previous Calendar implementation (not a separately recorded ADR).
