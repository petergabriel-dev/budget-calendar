# Calendar Selected-Day Transactions

**Status:** Implemented
**Surface:** Calendar page  
**Layer:** Feature composition

## Purpose

Give selected date compact transaction context while keeping Calendar focused on month navigation. Full-day browsing remains one explicit expand action.

## Contract

```kotlin
@Composable
fun CalendarSelectedDayTransactions(
    selectedDate: LocalDate,
    transactionCount: Int,
    items: List<TransactionListItemState>,
    isLoading: Boolean,
    errorMessage: String?,
    onTransactionClick: (Long) -> Unit,
    onViewAllClick: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Current compact Calendar density shows at most one inline item. Caller filters, sorts, maps account labels, and supplies amount/status meaning. Component performs no date math or transaction loading.

## Layout

1. Full-date section header: `Today · Jun 18` or `Tue · Jun 18`.
2. Non-interactive transaction count.
3. One compact `TransactionListItem` variant.
4. Accessible expand/view-all control.

- Header and list use vertical token spacing.
- Row uses existing `bgSurface`, radius, padding, and amount/status rules.
- Amount remains visible; leading text ellipsizes first.
- No duplicate page-level card.

## Behavior

- Selected date changes -> parent refreshes list.
- Row tap emits transaction id -> `TransactionDetailsSheet`.
- View all emits exact selected date -> full-day transaction view.
- Expand control remains available when zero, one, or many transactions exist; empty state still exposes no fake row.

## States

### Loading

Preserve date header and count context when available. Show one token-based row skeleton. Do not show `No transactions` while loading.

### Empty

Show `No transactions`. Keep section compact. Global Add action remains outside this component.

### Error

Show scoped inline error plus `Try again`. Preserve selected date and stale content only when caller explicitly supplies stale state.

### Content

Show one inline row. Full-day view owns remaining rows.

## Accessibility

- Header exposes selected date and transaction count.
- Row exposes title, description when present, account/direction, signed amount meaning, date context, and status.
- View-all label identifies date: `View all transactions for Tuesday, June 18`.
- Retry label identifies date: `Retry loading transactions for Tuesday, June 18`.
- All controls meet `spacing12` (48dp).
- Amount meaning never relies on color.

## Responsive

- Mobile-first, full available width.
- Single-column row layout with flexible leading content.
- Trailing amount and status remain visible.
- Full-day list scrolls inside available sheet width.
- No fixed row widths.

## Theme

Use `bgPrimary`, `bgSurface`, semantic text/amount/status tokens, focus ring, and existing transaction radius/spacing tokens in both themes.
