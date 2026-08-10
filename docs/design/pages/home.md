# Home Page

**Status:** Implemented
**Surface:** Main Home tab
**Layer:** Page composition

## Purpose

Give fast budget health, weekly activity, and transaction planning access. Home keeps Live Budget and Sandbox as two modes within one shared page shell.

## Page shell

```text
Scrollable content
    Header
    Live Budget / Sandbox switch
    Mode content

App shell bottom navigation remains outside page content.
```

- Background: `bgPrimary`.
- Horizontal and top insets: existing spacing tokens.
- Bottom inset accounts for app tab bar through a shared shell inset token.
- Single-column vertical flow.
- Header title is `Budget` in Live Budget mode and `Sandbox` in Sandbox mode.
- Header action is `BcNotificationBell`.
- Mode switch is `BcSegmentedControl` with `Live Budget` and `Sandbox`.

## Live Budget mode

```text
BcHeader("Budget")
BcSegmentedControl
HeroSafeToSpend
optional Available Credit metric
HomeWeeklyActivity
Overdue section
```

### Primary budget metric

- `HeroSafeToSpend` is first content block.
- Shows Safe to Spend amount.
- Shows daily rate and days remaining when available.
- Last-day warning uses existing warning/error treatment.

### Secondary credit metric

- Render only when available credit exists.
- Use shared metric-hero treatment with secondary hierarchy.
- Never compete visually with Safe to Spend.

### Weekly activity

Use `HomeWeeklyActivity` exactly as defined in its component spec:

```text
This Week
weekly spend summary
BcWeekPicker
SelectedDayTransactionPreview
```

### Overdue section

- Section title: `Overdue`.
- Pending count is non-interactive metadata, not an action.
- Empty state: `No overdue transactions`.
- Rows use shared transaction display rules with attention status visible.
- Row tap opens transaction detail/edit flow.

## Sandbox mode

```text
BcHeader("Sandbox")
BcSegmentedControl
SnapshotSelectorPill
HeroProjectedSpend
optional ComparisonDeltaRow
SandboxTransactionList
sandbox error feedback
```

- Snapshot selector remains first control after mode switch.
- Projected spend is primary Sandbox metric.
- Comparison delta appears only when comparison data exists.
- `SandboxTransactionList` owns empty state and Add action.
- Sandbox error appears inline below affected content.

## Actions and overlays

| Action | Result |
|---|---|
| Notification bell | Open `NotificationCenterSheet` |
| Add action in app tab bar | Open `TransactionFormSheet` |
| Overdue/preview row | Open `TransactionDetailsSheet` |
| Sandbox snapshot pill | Open `SnapshotSelectorSheet` |
| Sandbox Add | Open `AddSandboxTransactionSheet` |
| Promote sandbox transaction | Confirm, then promote |
| Remove sandbox transaction | Remove sandbox item |
| View all selected-day transactions | Switch to Calendar with selected date |

Overlays own their state and dismissal. Home only coordinates visibility and callbacks.

## State matrix

| Area | Loading | Empty | Error |
|---|---|---|---|
| Safe to Spend | Metric skeleton | Zero/neutral metric | Inline retry treatment |
| Available Credit | Omit until available | Omit | Omit; non-critical data |
| Weekly activity | Summary/preview skeletons | Zero spend + no transactions | Scoped retry |
| Overdue | Preserve section structure | `No overdue transactions` | Inline retry treatment |
| Sandbox snapshot | Selector placeholder | `No Snapshot Selected` | Inline error |
| Projected spend | Metric skeleton | Neutral zero state | Inline error |
| Sandbox transactions | List skeleton | Add transaction empty state | Inline error |

One failing section must not blank entire Home page.

## Hierarchy rules

1. Page identity and mode switch.
2. Current mode's primary metric.
3. Weekly or projected context.
4. Transaction detail and actions.
5. Errors and secondary metadata.

Do not wrap entire Home in a new card. Use surfaces only for existing cards, rows, sheets, and controls.

## Accessibility

- Header exposes current mode in title and switch selected state.
- Segmented control exposes tab/selectable semantics.
- Metric labels accompany all currency values.
- Amount meaning never relies on color alone.
- Transaction rows expose full title, account, amount, type, date, and status.
- Empty and error states are announced in their affected sections.
- All interactive controls meet `spacing12` touch target.

## Responsive

- Mobile-first, single-column vertical layout.
- Content uses flexible width and token insets.
- Hero values may scale down through typography rules; no fixed page width.
- Weekly picker uses seven flexible columns.
- Transaction rows keep amount visible while text ellipsizes.
- Sheets use full available width and scrollable content.
- Wider layouts may use bounded content width; do not create multi-column Home galleries.

## Theme and token rules

- Use existing color, typography, spacing, radius, and shadow tokens.
- No local raw dp or color values.
- `BudgetCalendarTheme` selects semantic light/dark values from system appearance; no user theme preference is stored.
- Preserve existing Outfit display hierarchy and Inter body hierarchy.

## Navigation handoff

Home exposes selected-day navigation to App shell:

```text
HomeWeeklyActivity.onViewAllClick(selectedDate)
    -> App selects Calendar
    -> Calendar applies selected date
    -> Calendar renders full transaction list
```

BudgetScreen and RecurringScreen remain unused and are outside this page composition.
