# Compact TransactionListItem

**Status:** Proposed
**Surface:** Home selected-day preview
**Layer:** Feature component variant

## Purpose

Provide denser transaction rows for Home without duplicating transaction display rules. Existing regular `TransactionListItem` remains unchanged for Calendar and full transaction lists.

## API change

Add density to existing component:

```kotlin
enum class TransactionItemDensity {
    Regular,
    Compact,
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    accountName: String,
    destinationAccountName: String? = null,
    modifier: Modifier = Modifier,
    density: TransactionItemDensity = TransactionItemDensity.Regular,
    onTap: () -> Unit,
)
```

`density` changes presentation only. Amount sign, transfer handling, color meaning, and tap behavior remain shared.

## Compact anatomy

- Whole row is one tappable surface.
- Leading column: category/type title, optional description, account or transfer direction.
- Trailing column: signed amount and attention status badge.
- Hide visible transaction date; selected-day header provides date context.
- Keep date in accessibility label.

## Visual rules

- Surface: `bgSurface` with `radius.lg`.
- Padding and gaps use existing spacing tokens.
- Minimum touch target uses `spacing12` (48dp).
- Title: `bodyMedium`, semibold, `textPrimary`.
- Description/account: `bodySmall`, `textSecondary` or `textTertiary`.
- Amount: `bodyMedium`, bold.
- Expense amount: `colorError`.
- Income amount: `colorSuccess`.
- Transfer amount: `colorInfo`.
- Show `BcBadge` for `Pending` and `Overdue`.
- Do not show `Confirmed` badge in compact visual treatment; expose status to accessibility.
- Preserve transfer direction: `source → destination`.
- Long text ellipsizes before amount column loses visibility.

## Behavior

- Tap anywhere on row -> `onTap`.
- Home maps tap to `TransactionDetailsSheet`.
- Detail sheet owns edit/delete actions.
- No internal state, loading, empty, or error handling.
- No date filtering, sorting, or transaction mapping inside row.

## Content rules

- Title: `Transfer` for transfers; otherwise category or `Uncategorized`.
- Description shown only when non-blank.
- Account label always shown when available.
- Amount uses existing signed-amount rules.
- Status badge is attention-only in compact mode.

## Accessibility

- Whole row exposes one actionable control.
- Label includes title, description when present, account/direction, amount, full date, transaction type, and status.
- Color never carries amount meaning alone; label states income, expense, or transfer.
- Touch target remains at least `spacing12`.
- Preserve visible focus treatment using theme interaction tokens.

## Responsive

- Mobile-first, full available width.
- Use flexible leading column and non-shrinking trailing amount column.
- No fixed row width.
- Compact density remains single-row layout; text truncates before amount disappears.
- Parent preview stacks rows vertically and keeps `View all` below them.

## Home integration

```text
SelectedDayTransactionPreview
    -> TransactionListItem(density = Compact)
    -> onTap(transaction.id)
    -> TransactionDetailsSheet
```

Use compact density for the first three selected-day transactions only. Keep regular density for Calendar expanded lists and other full-detail contexts.
