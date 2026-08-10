# Accounts Page

**Status:** Implemented
**Surface:** Main Accounts tab  
**Layer:** Page composition

## Purpose

Show total net worth, account balances grouped by spending purpose, credit availability, and a clear path to add or manage accounts. Accounts is a single-column inventory; detail and form actions belong to sheets.

## Page shell

```text
Scrollable content
    Header
    Total net worth
    Spending Pool
    Savings & Investments
    Credit Cards
    Add account

App shell bottom navigation remains outside page content.
```

- Background: `bgPrimary`.
- Header title: `Accounts`.
- Header action: `BcNotificationBell`.
- Use existing page insets and vertical spacing tokens.
- Do not wrap entire Accounts page in a new card.
- Keep sections stacked. Do not introduce a desktop card gallery.

## Composition

1. `BcHeader("Accounts")`.
2. `NetWorthHero`.
3. Optional `AccountsLoadError` scoped above affected content.
4. `Spending Pool` section.
5. `Savings & Investments` section.
6. `Credit Cards` section.
7. Full-width `Add account` action.

Empty page replaces sections 3–6 with the Accounts empty state while retaining net worth and Add account.

## Net worth hero

- Label: `TOTAL NET WORTH`.
- Amount uses primary metric hierarchy and cents-based currency formatting.
- Neutral amount treatment by default; negative meaning is explicit in accessible text and uses approved error semantic when required.
- Hero remains visible while list data loads.
- Do not show zero as fake loaded data when net worth is unavailable.

## Account sections

Each populated section has:

```text
Section title                         Section total
Account cards
```

### Spending Pool

- Section total sums accounts included in spending pool.
- Cards use `bgDark` high-contrast treatment with `textInverted` content.
- Card shows account type label, balance, and optional description.
- `Safe to Spend` badge communicates inclusion. Avoid duplicating page-level Safe to Spend calculation.
- Tap opens Account Details sheet.

### Savings & Investments

- Section total sums accounts excluded from spending pool.
- Cards use `bgSurface` with primary text and semantic account-type label.
- Card shows balance, description/name fallback, and account type.
- Cards remain stacked on all widths for predictable reading order.
- Tap opens Account Details sheet.

### Credit Cards

- Section total is total current owed balance, not available credit.
- Tile shows credit-card label, account name, available credit or `Limit not set`, owed amount, statement due context, and utilization when limit exists.
- Owed balance uses error semantic only when it communicates debt context; label `Owed` remains visible.
- Utilization bar is supporting data, not the only utilization signal; show percentage text.
- Tap opens Credit Card Details sheet. Payment and settings actions remain inside sheet.

## Add account

- Full-width primary button follows sections.
- Label: `Add account`.
- Opens `AccountFormSheet`.
- Empty state uses same action and wording.
- Button meets `spacing12` (48dp) touch target.

## State matrix

| Area | Loading | Empty | Error |
|---|---|---|---|
| Net worth | Label plus metric skeleton | Neutral/unknown state | Inline metric error; list remains usable when available |
| Account list | Preserve section shell with card skeletons | `Add your first account` + `Start by adding where your money lives.` | Inline retry/feedback; preserve stale cards |
| Credit cards | Tile skeletons | Omit section when no cards | Scoped credit-card error; non-credit accounts remain usable |
| Add action | Available unless form submission is active | Primary empty-state action | Remains available after load failure |

State precedence is local. A credit-card failure must not blank ordinary accounts. Do not rely on snackbar as the only error communication for persistent load failure.

## Empty state

When no ordinary accounts and no credit cards exist:

```text
TOTAL NET WORTH
₱0.00 or neutral unavailable state

Add your first account
Start by adding where your money lives.

Add account
```

Center only empty-state content; page background remains `bgPrimary`. Do not add decorative illustration in this pass.

## Actions and sheets

| Action | Result |
|---|---|
| Notification bell | Open `NotificationCenterSheet` |
| Account card | Open Account Details sheet |
| Credit card tile | Open Credit Card Details sheet |
| Add account | Open `AccountFormSheet` |
| Edit account | Open AccountFormSheet with existing data |
| Delete account | Confirm, then remove account |
| Make payment | Open `CreditCardPaymentSheet` |
| Credit card settings | Open `CreditCardSettingsForm` |

Cards do not expose edit/delete directly. Details sheets own destructive-action confirmation and dismissal.

## Accessibility

- Header exposes Accounts page identity and notification action.
- Net worth label, formatted amount, and negative meaning form one metric description.
- Each account card is one actionable control with account name, type, balance, inclusion status, and description when present.
- Credit tile exposes available credit/limit, owed amount, due context, and utilization percentage.
- Utilization meaning does not rely on progress-bar color.
- Section totals are metadata, not controls.
- Empty, loading, and error states are announced in affected sections.
- All controls meet `spacing12` (48dp).
- Preserve visible focus and pressed treatment in both themes.

## Responsive

- Mobile-first, single-column vertical layout.
- Account cards use full available width; no fixed card widths.
- Long names/descriptions ellipsize before balances lose visibility.
- Credit-card secondary content may wrap; available credit and owed labels remain explicit.
- Wider layouts may use bounded content width but remain one vertical reading order.
- Sheets use full available width and scrollable content.

## Theme and token rules

- Use semantic color, typography, spacing, radius, shadow, and interaction tokens.
- `Spending Pool` high-contrast cards use `bgDark` + `textInverted`.
- Savings/investment surfaces use `bgSurface`.
- Credit utilization uses semantic accent plus visible percentage text.
- No local raw dp, px, or color values in component specs.
- Validate high-contrast spending cards, debt/error text, badges, borders, disabled states, and focus in light/dark themes.

## Shipped implementation notes

- Account and credit-card sections use tokenized stacked layouts at every width; no wide-layout grid branch remains.
- Account, net-worth, and credit-card failures render in their affected sections with retry while usable sibling content remains visible.
- Account cards use tap/details routing; destructive and edit actions live in details/confirmation flows rather than long press.
- Credit-card data remains separately owned and excluded from ordinary-account balance/net-worth formulas.
