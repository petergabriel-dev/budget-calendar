# Credit Card Summary Tile

**Status:** Implemented
**Surface:** Accounts page  
**Layer:** Feature component

## Purpose

Summarize one credit card's available credit, owed balance, statement context, and utilization. Keep payment/settings actions inside Credit Card Details sheet.

## Contract

```kotlin
@Composable
fun CreditCardSummaryTile(
    accountName: String,
    availableCredit: Long?,
    currentBalance: Long,
    creditLimit: Long?,
    statementDueAmount: Long?,
    statementDueDateLabel: String?,
    utilizationPercent: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

All amounts are cents. Caller supplies formatted date context and derived utilization. Component does no credit calculations.

## Anatomy

```text
CREDIT CARD                                  account name

Available credit or Limit not set
Owed ₱X · due ₱Y due Jun 24 · of ₱Z
Utilization bar + Utilization N%
```

- Surface: `bgSurface` with existing radius and spacing.
- Credit-card label uses a non-interactive badge/treatment.
- Available credit is primary tile value.
- `Owed` label stays explicit; amount uses approved debt/error semantic when appropriate.
- Omit utilization bar when no positive limit exists.
- Show visible percentage text whenever utilization is shown.

## States

### Limit unavailable

Show `Limit not set`. Keep owed balance and due context visible. Omit utilization.

### Loading

Preserve tile shell with label, amount, and supporting skeletons. Expose progress semantics.

### Error

Keep unaffected account sections usable. Show scoped credit-card error/retry owned by page state.

## Behavior

Tap opens Credit Card Details sheet. Payment, edit settings, and delete actions are sheet-owned. Tile has no nested action.

## Accessibility

One actionable label includes account name, available credit/limit state, owed balance, due date/amount, and utilization percentage. Progress bar is supplemental; percentage is announced as text. Minimum touch target: `spacing12` (48dp).

## Responsive

- Mobile-first, full available width.
- Supporting text wraps naturally; primary amount remains visible.
- No fixed tile width.
- Tiles remain stacked at wider widths.

## Theme

Use `bgSurface`, `borderStrong`, `textPrimary`, `textSecondary`, `colorErrorText`, `bgDark`, radius, spacing, and focus tokens. Validate debt, unavailable-limit, progress, and disabled contrast in both themes.
