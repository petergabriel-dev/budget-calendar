# Sandbox Mode

**Status:** Proposed  
**Surface:** Home page, Sandbox mode  
**Layer:** Feature composition

## Boundary

Sandbox is a mode inside Home, not a new top-level tab or page. It shares Home header, notification action, and Live Budget/Sandbox segmented control.

```text
Home header
Live Budget / Sandbox switch
    Sandbox mode
        Snapshot selector
        Projected spend
        Optional comparison
        Sandbox transactions
```

Keep `BudgetScreen` and `RecurringScreen` unused. Do not add a Sandbox bottom-navigation destination.

## Composition

1. Shared `BcHeader("Sandbox")`.
2. Shared mode switch with Sandbox selected.
3. `SandboxSnapshotSelector`.
4. `BcMetricHero` Projected Spend variant.
5. Optional `ComparisonDeltaRow`.
6. `SandboxTransactionList`.

- Background: `bgPrimary`.
- Single-column vertical flow.
- Snapshot selector is first control after mode switch.
- No new outer page card.

## Snapshot selector

- Full-width pill uses `bgDark` + `textInverted`.
- Active snapshot name is visible.
- No active snapshot label: `No Snapshot Selected`.
- Accessible action: `Select sandbox snapshot`.
- Opens `SnapshotSelectorSheet`.
- Sheet supports select, create new, and delete with confirmation.
- Active state uses visible check treatment plus accessible `Active snapshot` label.

## Projected spend

- Label: `PROJECTED SPEND`.
- Primary metric hierarchy through `BcMetricHero`.
- Amount and daily rate use cents formatting.
- Positive/neutral projection remains text-primary.
- Zero/negative projection uses error semantic and explicit warning text. Icon is optional support, never the only signal.
- Projection calculation remains in ViewModel/use case. Hero performs no simulation math.

## Comparison

- Render only when comparison data exists.
- Leading text: `vs ₱X real`.
- Trailing delta includes sign: `+₱X`, `−₱X`, or neutral zero.
- Delta meaning is stated in text and supported by semantic tone; color is never the only signal.
- Comparison disappears when no active snapshot or comparison data is available.

## Sandbox transactions

Section header:

```text
Sandbox Transactions                         Add
```

Rows show:

- Type badge: `Income`, `Expense`, or `Transfer`.
- Category/name with `Uncategorized` fallback.
- Optional description.
- Date.
- Signed amount with type meaning.
- `Promote` action only when transaction has not already been promoted.
- `Remove` destructive action.

- Rows use `bgSurface`, existing radius, spacing, and transaction semantics.
- Row actions remain separate controls with minimum touch targets.
- Promote opens confirmation: `This adds the transaction to real data and removes it from sandbox.`
- Remove opens confirmation when destructive risk is not obvious from current context.
- Promoted transactions expose promoted state; do not show a misleading Promote action.

## Empty and no-snapshot states

### No snapshot selected

```text
No Snapshot Selected
Choose or create a sandbox snapshot to start planning.
Select snapshot
```

Do not show zero as a loaded projection.

### No transactions

```text
No transactions yet. Tap Add to start planning.
Add
```

Keep Add action available. Do not hide the section header.

## Actions and sheets

| Action | Result |
|---|---|
| Snapshot pill | Open SnapshotSelectorSheet |
| Select snapshot | Load snapshot and close sheet |
| Create New | Open create-name dialog, then select created snapshot |
| Delete snapshot | Confirm, delete, select next available snapshot |
| Add | Open AddSandboxTransactionSheet |
| Promote | Confirm, promote to real data, remove from sandbox |
| Remove | Confirm/remove sandbox transaction |
| Mode switch to Live Budget | Clear Sandbox mode-local sheet state and return Live Budget content |

Sheets own their input, confirmation, dismissal, and validation state. Sandbox page only coordinates callbacks.

## State matrix

| Area | Loading | Empty | Error |
|---|---|---|---|
| Snapshot selector | Pill placeholder; preserve mode switch | `No Snapshot Selected` | Inline snapshot error with retry/select action |
| Projected spend | Metric skeleton | No snapshot state, not zero metric | Scoped projection error; transactions remain when available |
| Comparison | Omit until data exists | Omit | Omit; projection remains usable |
| Transactions | List skeleton | Add transaction empty state | Inline transaction error; snapshot/hero remain usable |
| Add sheet | Submit busy state | Validation near fields | Save error stays in sheet |

Local state precedence is `loading > error > empty/content`. A transaction failure must not blank selected snapshot or projected spend.

## Data and calculation rules

- Amounts are cents (`Long`).
- ViewModel owns snapshot selection, projected safe-to-spend calculation, daily-rate calculation, comparison, and mutations.
- Component owns only visual hierarchy, formatting delegation, and callbacks.
- Daily rate uses current days-remaining rule supplied by domain state; no UI date math.
- Snapshot expiry is handled before selection; expired snapshots must not render as active.

## Accessibility

- Shared mode switch exposes current Sandbox selection.
- Snapshot pill exposes active name and selection action.
- Projected metric exposes label, amount, daily rate, and negative/warning meaning as one description.
- Comparison exposes real value, sandbox value/delta meaning, and direction in text.
- Transaction row/action labels include type, category, date, signed amount, and promoted state.
- Promote and Remove remain individually reachable and clearly labeled.
- Empty/error states are announced in affected sections.
- All controls meet `spacing12` (48dp).
- Preserve focus after sheet dismissal and after list mutation when possible.

## Responsive

- Mobile-first, single-column layout.
- Snapshot pill, hero, comparison, list, and Add action use full available width.
- Row leading content ellipsizes before signed amount/actions lose visibility.
- Action buttons may stack vertically at narrow widths; do not shrink below touch target.
- Wider layouts remain one vertical reading order; no Sandbox card gallery.

## Theme and token rules

- Use semantic surface, text, amount, status, border, focus, spacing, radius, and shadow tokens.
- Selector uses `bgDark` + `textInverted`.
- Expense/negative uses error semantics, income/positive uses success semantics, transfer uses info semantics.
- Every colored status has a text/badge counterpart.
- No raw local dp, px, or color values in component specs.
- Validate selector, negative hero, destructive action, badges, disabled controls, and focus in both themes.

## Known implementation gaps

- Current Sandbox is already embedded in Home state, but Home preview/implementation must keep the segmented control as mode switch rather than creating a route.
- Current rows place Promote/Remove actions inside each item; Build should preserve clear action grouping and confirmation semantics.
- Current errors are surfaced through shared state; Design requires affected-content feedback rather than blanking all Sandbox content.
- Current components contain local size values; Build migration should replace them with tokens and shared `BcMetricHero`/badge rules.
