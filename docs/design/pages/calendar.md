# Calendar Page

**Status:** Implemented
**Surface:** Main Calendar tab  
**Layer:** Page composition

## Purpose

Show month-level budget activity, projected end-of-month balance, and the selected day's transactions. Calendar owns date navigation and full-day transaction access. Home can hand off one exact selected date.

## Page shell

```text
Scrollable content
    Header
    Month navigation
    End-of-month projection
    Month grid
    Selected-day section

App shell bottom navigation remains outside page content.
```

- Background: `bgPrimary`.
- Use existing horizontal, top, and bottom shell inset tokens.
- Single-column vertical flow.
- Header title: `Calendar`.
- Header action: `BcNotificationBell`.
- Previous/next month buttons are primary navigation. Horizontal swipe is an optional equivalent gesture, never the only path.
- Do not wrap entire Calendar in a new card.

## Composition

1. `BcHeader("Calendar")`.
2. `CalendarMonthNavigation`.
3. `EndOfMonthProjectionPill`.
4. `CalendarMonthGrid`.
5. `CalendarSelectedDayTransactions`.

### Month navigation

- Month label uses abbreviated month plus year, for example `Jun 2024`.
- Previous and next controls expose `Previous month` and `Next month` labels.
- Month change selects same day when valid; otherwise state chooses a valid date in target month.
- Adjacent-month cells remain visible in the six-week grid and are tappable. Tapping one navigates to its month and selects that exact date.
- Navigation preserves selected date context; loading affects month data only.

### End-of-month projection

- Full-width `EndOfMonthProjectionPill` follows month navigation.
- Label: `End of Month Projection`.
- Amount is formatted from cents and remains readable in both themes.
- Negative or zero projection uses `colorErrorText` plus explicit text meaning; never color alone.
- Projection loading/error is scoped to pill. It must not blank month grid or selected-day content.

### Month grid

- Seven equal columns ordered Sunday through Saturday.
- Always renders 42 cells for stable layout.
- Current-month cells use normal emphasis. Adjacent-month cells use reduced emphasis while remaining actionable.
- Selected day uses `bgDark` and `textInverted`.
- Today uses a visible `focusRing`/`bgDark` outline when not selected. Selected state wins visually, with today retained in accessibility text.
- Daily summaries may show positive/negative tint plus status dots for pending and overdue.
- Net amount, pending, or overdue meaning must be available in the day accessibility label; dots and tint are supporting signals.
- Empty days remain selectable with no summary decoration.

### Selected-day section

- Header uses full date context: `Today · Jun 18` or `Tue · Jun 18`.
- Transaction count is non-interactive metadata.
- Compact list shows one transaction inline in current Calendar density. Reuse `TransactionListItem` rules.
- Expand action opens full selected-day transaction sheet/list. Accessible label: `View all transactions for Tuesday, June 18`.
- Row tap opens `TransactionDetailsSheet`.
- Empty state: `No transactions`.
- Do not duplicate global Add action inside empty selected-day state.

## State matrix

| Area | Loading | Empty | Error |
|---|---|---|---|
| Month grid | Preserve month shell; show cell skeleton treatment | Render 42 neutral cells with no summaries | Inline month error with retry; preserve navigation |
| Projection | Pill skeleton | Neutral zero/unknown state | Inline projection error; grid remains usable |
| Selected-day list | Preserve date header; show compact row skeleton | `No transactions` | Inline selected-day error with retry |
| Accounts metadata | Keep fallback account label only when supplied | No extra state | Do not blank transaction rows |

State precedence is local to each area. One failing area must not blank Calendar page.

## Interaction contract

```kotlin
CalendarMonthGrid.onDateSelected(date)
    -> Calendar selects exact LocalDate
    -> month reloads when date is outside current month
    -> selected-day transactions refresh

CalendarSelectedDayTransactions.onViewAllClick(selectedDate)
    -> open full selected-day transaction view
```

- Month navigation updates month data and projection.
- Day selection updates selected-day list without changing projection.
- Swipe navigation and arrow buttons use the same month navigation callback.
- Details, edit, delete, and form state remain owned by their sheets/ViewModels.

## Home handoff

Home emits selected-day id/date through its existing callback:

```text
Home View all transactions
    -> App selects Calendar tab
    -> Calendar receives selected date
    -> Calendar loads containing month
    -> Calendar marks exact date selected
```

Calendar must not default back to today's date after a Home handoff. Selected date is state, not a visual hint.

## Accessibility

- Header exposes `Calendar` page identity and notification action.
- Month navigation buttons expose direction and target month.
- Every day cell is one actionable control with full date label, current-month context, selected/today state, net summary when present, pending/overdue status, and transaction count.
- Selected day is exposed as selected; exactly one cell is selected.
- Projection label and formatted amount form one coherent metric description.
- Selected-day count is metadata, not an action.
- Expand action identifies selected date.
- Transaction rows expose title, account/direction, amount meaning, date context, and status.
- Empty, loading, and error states are announced in affected sections.
- All interactive controls meet `spacing12` (48dp) touch target.
- Preserve visible focus and pressed treatment in both themes.

## Responsive

- Mobile-first, single-column vertical layout.
- Grid uses seven flexible equal columns; no fixed page, cell, or row widths.
- Day number and status indicators remain readable at narrow widths.
- Selected-day rows keep trailing amount visible while leading text ellipsizes.
- Selected-day full list uses available width and scrolls independently when presented in a sheet.
- Wider layouts may use a bounded content width but never create side-by-side Calendar galleries.

## Theme and token rules

- Consume semantic color, typography, spacing, radius, and interaction tokens.
- No local raw dp, px, or color values in component specs.
- `BudgetCalendarTheme` supplies semantic light/dark values from system appearance; no user theme preference is stored.
- `selected` uses `bgDark` + `textInverted`.
- Projection negative uses `colorErrorText`; positive/negative day tint uses approved semantic accents.
- Status dots must be paired with accessible text and not act as the only status signal.

## Shipped implementation notes

- `CalendarScreen` uses date-specific accessible rows and expands through the shared sheet shell; row taps open details before edit/delete actions.
- Day cells, projection, month controls, and selected-day states use semantic tokens and scoped retry/error feedback.
- `AppNavigationState` carries Home's exact selected date into Calendar; Calendar applies the containing month and consumes the handoff.

BudgetScreen and RecurringScreen remain unused and outside this page composition.
