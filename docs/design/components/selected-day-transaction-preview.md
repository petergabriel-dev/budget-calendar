# SelectedDayTransactionPreview

**Status:** Proposed
**Surface:** Home page, Live Budget mode
**Layer:** Feature component

## Purpose

Show selected day's transactions without turning Home into Calendar. `BcWeekPicker` owns day selection; this component renders already-filtered selected-day data.

## Anatomy

1. Header with selected date and non-interactive transaction count.
2. Up to three compact transaction rows.
3. Full-width `View all` button when more transactions remain.

Use `BcSectionHeader` styling. Count is metadata, not an action.

## Contract

```kotlin
data class SelectedDayTransactionPreviewItem(
    val id: Long,
    val title: String,
    val description: String?,
    val accountLabel: String,
    val amountLabel: String,
    val amountTone: AmountTone,
    val statusLabel: String?,
    val accessibilityLabel: String,
)

@Composable
fun SelectedDayTransactionPreview(
    dateLabel: String,
    transactionCount: Int,
    items: List<SelectedDayTransactionPreviewItem>,
    hasMore: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onTransactionClick: (Long) -> Unit,
    onViewAllClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
)
```

`items` contains zero to three entries. Caller handles date filtering, cancelled-transaction exclusion, sorting, account labels, and amount formatting. Component performs no domain logic.

## Compact row

- Surface: `bgSurface`, `radius.lg`.
- Padding: existing `spacing3`.
- Leading content: title, optional description, account/direction label.
- Trailing content: signed amount with transaction-type tone.
- Show status badge for pending or overdue; confirmed status remains available to accessibility text without adding visual noise.
- Expense: error tone. Income: success tone. Transfer: info tone.
- Hide row date; selected-date header already provides context.
- Row tap opens `TransactionDetailsSheet`; component emits id only.

Reuse `TransactionListItem` behavior. Add compact density/metadata treatment rather than cloning transaction rules.

## View all

- Render only when `hasMore` is true.
- Use full-width `BcButton` Ghost variant.
- Label: `View all transactions` or include count when useful.
- Button opens full selected-day transaction view, normally Calendar with selected date.
- Keep navigation outside component through `onViewAllClick`.

## States

### Loading

Render up to three token-based skeleton rows. Expose progress semantics. Do not show empty state while loading.

### Empty

Show `No transactions this day`. Keep section height compact. No duplicate add action; global Home add action remains available.

### Error

Show inline error message with `Try again` Ghost button. Preserve selected date and existing content when stale data exists.

### Content

Show zero to three rows. Show `View all` only when additional rows exist.

## Accessibility

- Header exposes selected date and total count.
- Each row is one actionable control with full label, including title, amount, account, and status.
- View-all label identifies selected date: `View all transactions for Monday, June 17`.
- Retry label identifies operation: `Retry loading transactions for Monday, June 17`.
- Minimum touch target: `spacing12` (48dp).
- Amount meaning must not rely on color; accessibility label includes income, expense, or transfer meaning.

## Responsive

- Mobile-first, full available width.
- Rows remain single-column with flexible text width and trailing amount.
- Long titles/descriptions ellipsize; amount stays visible.
- View-all button remains full width.
- On wider layouts, component may use bounded parent width; never introduce fixed row widths or side-by-side preview layout.

## Home integration

```text
BcWeekPicker selected id
    -> Home selected date
    -> filter visible transactions for date
    -> map first three items + hasMore
    -> SelectedDayTransactionPreview
```

Row tap opens transaction detail. View all opens full selected-day transaction view. Empty, loading, and error states remain scoped to selected date.
