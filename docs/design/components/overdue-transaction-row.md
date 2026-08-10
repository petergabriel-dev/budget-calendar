# OverdueTransactionRow

**Status:** Implemented
**Surface:** Home page, Live Budget mode
**Layer:** Feature component variant

## Purpose

Present overdue transactions as attention items with clear status, relative date context, amount, and detail access. Replace Home-local `ScheduleTransactionRow`.

## Contract

```kotlin
@Composable
fun OverdueTransactionRow(
    transaction: Transaction,
    accountName: String,
    destinationAccountName: String? = null,
    relativeDateLabel: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
)
```

Caller filters overdue transactions, excludes cancelled data, sorts rows, and provides localized relative date text. Component performs no date math or transaction loading.

## Anatomy

- Whole row is one tappable control.
- Leading column: title, optional description, account or transfer direction.
- Supporting metadata: relative date such as `2 days ago`.
- Trailing column: signed amount and required `Overdue` badge.
- Expense amount uses error tone. Transfer and income preserve shared transaction amount rules.
- Use existing compact transaction layout; overdue row adds visible date and always-visible status.

## Visual rules

- Surface and radius follow compact `TransactionListItem` treatment.
- Use existing spacing tokens for padding and gaps.
- Use `TransactionStatusBadge(TransactionStatus.OVERDUE)`.
- Amount uses existing signed-amount formatting.
- Relative date uses `bodySmall` and secondary/tertiary text color.
- No decorative circle or raw divider required; row separation uses existing list treatment.
- Long title/description truncates before amount and status lose visibility.

## Behavior

- Tap row -> `TransactionDetailsSheet`.
- Detail sheet owns edit, delete, and status actions.
- No direct edit from row.
- No internal state, loading, empty, or error handling.
- Preserve stable row key through transaction id.

## Content rules

- Title: `Transfer` for transfers; otherwise category or `Uncategorized`.
- Description shown only when non-blank.
- Account label always shown when available.
- Transfer label preserves `source → destination`.
- Relative date is visible and also included in accessibility text.
- Overdue status is always visible and always announced.

## Accessibility

- Whole row exposes one actionable control.
- Label includes title, description when present, account/direction, relative date, amount, transaction type, and `Overdue` status.
- Amount meaning does not rely on color alone.
- Minimum touch target: `spacing12` (48dp).
- Preserve visible focus and pressed treatment using theme interaction tokens.

## Responsive

- Mobile-first, full available width.
- Flexible leading column; amount and status remain visible.
- No fixed row width.
- Text ellipsizes before trailing content collapses.
- Rows stack vertically within Overdue section.

## Home integration

```text
transactionState.transactions
    -> filter status == OVERDUE
    -> exclude CANCELLED
    -> sort oldest due first
    -> OverdueTransactionRow
    -> TransactionDetailsSheet
```

Keep pending count as non-interactive metadata in Overdue section header. Replace `ScheduleTransactionRow` after migration.
