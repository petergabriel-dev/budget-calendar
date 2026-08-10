# Calendar Month Grid

**Status:** Proposed  
**Surface:** Calendar page  
**Layer:** Feature component

## Purpose

Render one navigable month as a stable seven-column grid. Keep date selection, daily summaries, and calendar accessibility together without owning transactions or navigation policy.

## Contract

```kotlin
data class BcCalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val netAmount: Long?,
    val transactionCount: Int,
    val hasPending: Boolean,
    val hasOverdue: Boolean,
)

@Composable
fun CalendarMonthGrid(
    days: List<BcCalendarDay>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
)
```

`days` contains exactly 42 ordered entries, Sunday first. Caller supplies summaries and date state. Component performs no date math, filtering, or month navigation.

## Anatomy

```text
Weekday labels: Sun Mon Tue Wed Thu Fri Sat
42 day cells in six rows
```

Each cell contains:

- Day number.
- Optional positive/negative daily tint.
- Optional pending and overdue status dots.
- Selected/today treatment.

## Visual rules

- Seven equal flexible columns.
- Cell surface follows `bgPrimary`; selected cell uses `bgDark` + `textInverted`.
- Adjacent-month cells use reduced emphasis, not disabled treatment.
- Today uses a visible `focusRing`/`bgDark` outline unless selected.
- Positive/negative tints use approved semantic accents with restrained opacity.
- Pending dot uses `colorWarning`; overdue dot uses `colorError`.
- No raw dividers or decorative borders on every cell.
- Keep day number visually primary; indicators remain secondary.

## Behavior

- Tapping any cell emits exact `LocalDate`.
- Adjacent-month tap emits date; parent navigates month and retains exact selection.
- Exactly one selected cell is supplied by caller.
- Cell does not open transaction details directly.

## Accessibility

Each cell label includes:

```text
Tuesday, June 18, selected, 2 transactions, pending and overdue
```

Omit unavailable summary/status clauses. Include `today` when applicable. Do not rely on tint or dots alone. Minimum touch target: `spacing12` (48dp).

## Responsive

- Mobile-first, full available width.
- Seven flexible columns; no fixed cell width.
- Day numbers and status indicators remain readable at narrow widths.
- Grid remains six rows for stable vertical rhythm.

## States

- Loading: preserve 42-cell grid with token skeletons or neutral placeholders.
- Empty month: neutral cells with no status indicators.
- Error: parent shows scoped retry while grid shell remains available when possible.

## Theme

Use semantic tokens only. Validate selected, today, adjacent-month, positive, negative, pending, overdue, and focus treatments in light and dark themes.
