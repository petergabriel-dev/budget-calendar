# Account Balance Card

**Status:** Implemented
**Surface:** Accounts page  
**Layer:** Feature component

## Purpose

Present one deposit, cash, savings, investment, or spending-pool account as a tappable balance summary. Keep account grouping and balance meaning clear without duplicating account detail actions.

## Contract

```kotlin
@Composable
fun AccountBalanceCard(
    accountName: String,
    accountTypeLabel: String,
    balance: Long,
    description: String?,
    group: AccountBalanceCardGroup,
    isInSpendingPool: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Balance is cents. Caller owns account model mapping, grouping, and updates.

## Variants

### Spending Pool

- Surface: `bgDark`.
- Content: `textInverted`.
- Account type appears in a compact label treatment.
- Balance is primary card content.
- Description is optional and secondary.
- `Safe to Spend` badge remains visible.

### Savings & Investments

- Surface: `bgSurface`.
- Content: `textPrimary`.
- Account type label uses semantic supporting treatment.
- Balance remains primary; description falls back to account name when absent.

## Layout

```text
Account type label                         balance

Balance or account name context
Description/account fallback

Safe to Spend or Excluded badge
```

- Full available width.
- Use existing card radius and spacing tokens.
- Keep balance visible when name/description truncates.
- Card is one action; no nested controls.

## Behavior

Tap opens Account Details sheet. Edit/delete actions remain in sheet. Component has no internal state, loading, or error handling.

## Accessibility

Card action label includes account name, type, balance, description when present, and spending-pool inclusion status. Amount meaning does not rely on color. Minimum touch target: `spacing12` (48dp). Preserve focus and pressed treatment.

## Responsive

- Mobile-first, full available width.
- No fixed card width.
- Long names/descriptions ellipsize before balance is hidden.
- Cards remain stacked at wider widths; do not create a gallery grid.

## Theme

Validate high-contrast `bgDark` card and `bgSurface` card in both themes. Use semantic tokens only.
