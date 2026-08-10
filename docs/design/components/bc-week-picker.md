# BcWeekPicker

**Status:** Proposed
**Surface:** Home page, Live Budget mode
**Layer:** Foundation component

## Purpose

Select one day from current seven-day week. Selection drives Home compact transaction preview. Component stays stateless; Home owns selected date and transaction data.

## Anatomy

- Seven equal-width day items in chronological order.
- Weekday label: short, localized label such as `MON`.
- Day number: numeric calendar day.
- Activity indicator: optional dot when day has transactions.
- Selected container: `bgDark` with `textInverted` content.
- Today indicator: subtle outline when today is not selected.

## Contract

```kotlin
data class BcWeekDay(
    val id: String,
    val weekdayLabel: String,
    val dayOfMonth: Int,
    val accessibilityLabel: String,
    val isSelected: Boolean,
    val isToday: Boolean = false,
    val hasTransactions: Boolean = false,
    val enabled: Boolean = true,
)

@Composable
fun BcWeekPicker(
    days: List<BcWeekDay>,
    onDaySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

Caller provides exactly seven ordered entries. Component performs no date math, filtering, or transaction loading.

## Behavior

- Exactly one day selected in normal state.
- Tap enabled day -> call `onDaySelected(id)`.
- Selected day remains selected after recomposition through caller state.
- Disabled day ignores input and uses disabled color.
- Activity dot communicates transactions only; it does not replace selected state.
- Home renders compact transaction preview below picker for selected day.

## Visual states

| State | Container | Text | Indicator |
|---|---|---|---|
| Default | transparent | `textSecondary` | `colorInfo` dot when active |
| Selected | `bgDark` | `textInverted` | `textInverted` dot when active |
| Today | transparent | `textPrimary` | `borderStrong` outline |
| Today + selected | selected | selected colors | selected treatment wins |
| Disabled | transparent | `textDisabled` | hidden |
| Pressed/focused | token-based interaction treatment | unchanged | unchanged |

Use existing spacing, radius, typography, and color tokens. No local dp, raw colors, or date-specific styling.

## Accessibility

- Each day is a selectable control with `Role.Tab` semantics.
- Expose full label, for example: `Monday, June 17, selected, 2 transactions`.
- Expose selected state through semantics, not color only.
- Minimum touch target: `spacing12` (48dp).
- Support keyboard/focus navigation where platform supports it.
- Activity indicator must not be sole source of transaction information; include count in accessibility label.

## Responsive

- Mobile-first, full available width.
- Each day uses equal flexible width; no fixed item width.
- Keep labels short in narrow layouts; accessibility label remains full.
- Preserve touch target when width becomes constrained.
- On wider layouts, component may use a bounded parent width; never force page-wide fixed width.
- Compact transaction preview remains stacked below picker, never beside it.

## Home integration

```text
weekStart + timezone -> seven BcWeekDay entries
selected day -> Home state
selected day -> compact transaction preview
```

Place `BcWeekPicker` below `BcSectionHeader("This Week")`. Render preview below picker. Empty preview state: `No transactions this day`.
