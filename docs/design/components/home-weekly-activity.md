# Home Weekly Activity

**Status:** Proposed
**Surface:** Home page, Live Budget mode
**Layer:** Feature composition

## Purpose

Connect weekly spending summary, day selection, and selected-day transaction preview. Keep Home compact while giving selected-day data a clear next step.

## Composition

```text
BcSectionHeader("This Week")
Weekly spend summary
BcWeekPicker
SelectedDayTransactionPreview
```

No new outer card. Section uses Home `bgPrimary` and existing token spacing.

## Contract

```kotlin
data class WeeklySpendSummaryState(
    val amount: Long,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class SelectedDayTransactionPreviewState(
    val dateLabel: String,
    val transactionCount: Int,
    val items: List<SelectedDayTransactionPreviewItem>,
    val hasMore: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@Composable
fun HomeWeeklyActivity(
    weeklySpend: WeeklySpendSummaryState,
    weekDays: List<BcWeekDay>,
    selectedDayPreview: SelectedDayTransactionPreviewState,
    onDaySelected: (String) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewAllClick: (String) -> Unit,
    onRetryWeeklySpend: () -> Unit,
    onRetryPreview: () -> Unit,
    modifier: Modifier = Modifier,
)
```

`weekDays` contains seven ordered entries with exactly one selected day. `weeklySpend.amount` remains cents. `selectedDayPreview.dateLabel` matches selected day. `selectedDayPreview.items` contains zero to three entries. `onViewAllClick` receives selected-day id; parent maps it to Calendar navigation. Component performs no date math, filtering, or loading.

## Layout

1. `BcSectionHeader` title: `This Week`.
2. Weekly spend summary:
   - Currency amount from `weeklySpend.amount` cents, using `typography.cardTitle` with bold emphasis.
   - Supporting label: `Spent This Week`, using `typography.bodySmall` and secondary color.
   - Summary does not change when selected day changes.
3. `BcWeekPicker`.
4. `SelectedDayTransactionPreview` for current selected day.

Use vertical token spacing. Keep summary before picker so aggregate context precedes drilldown.

## Interaction

- Initial selected day: current day supplied by Home state.
- Day selection -> `onDaySelected(id)` -> update selected day -> refresh preview.
- Preview row -> `onTransactionClick(id)` -> transaction detail sheet.
- View all -> `onViewAllClick(selectedDayId)` -> Calendar opens selected date.
- Picker selection does not alter weekly spend total.

## States

### Summary loading

Show token-based amount skeleton while preserving section structure. Keep week picker usable when day data exists.

### Summary error

Show `Unable to load weekly spending` with `Try again` action. Keep picker and preview available when their data exists.

### Preview loading

Delegate to `SelectedDayTransactionPreview` loading state. Do not replace weekly summary.

### Preview empty

Show `No transactions this day` below picker.

### Preview error

Show inline retry within preview. Preserve selected day and weekly total.

### No weekly transactions

Show zero currency amount with normal summary treatment. Preview remains available for day selection.

## Accessibility

- Section exposes heading `This Week`.
- Summary exposes amount and `Spent This Week`.
- Week picker exposes selected day and activity counts.
- Preview exposes selected date, transaction count, row labels, and View all action.
- View-all action identifies selected date.
- All controls use minimum `spacing12` touch targets.

## Responsive

- Mobile-first, single-column vertical layout.
- Weekly amount uses fluid available width.
- Week picker uses seven equal flexible columns.
- Preview rows stack vertically.
- `View all` remains full width.
- No fixed page, card, or row widths.
- Wider layouts may use bounded parent width; do not place preview beside picker.

## Navigation handoff

Current App shell switches tabs but does not pass selected date. Home integration requires a parent callback that selects Calendar and applies selected-day id/date before rendering.

```text
HomeWeeklyActivity
    -> onViewAllClick(selectedDayId)
    -> App selects Calendar
    -> Calendar selects matching date
```
