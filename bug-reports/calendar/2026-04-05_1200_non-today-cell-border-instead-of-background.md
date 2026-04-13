---
feature: Calendar
date: 2026-04-05
---

## Description

Days with a positive net amount (non-today, non-selected) are rendered with a **green border outline** rather than a **green background fill**. The user wants the net-amount indicator to be expressed as a background color (same visual language as the "today" cell), not a border. The black border should be reserved exclusively as the "today" indicator.

**Reproduction:**
1. Open the Calendar screen.
2. Navigate to any month that contains past or future dates with confirmed/pending income exceeding expenses.
3. Observe that days like April 2 render with a green rounded-rectangle border and a transparent background.

**Expected:** Light green background (colorSuccess @ ~20% alpha), no colored border.
**Actual:** Green border, transparent background.

**Affected persona:** All users who have transactions on non-today, non-selected calendar days.

---

## Severity

**Low** — This is a pure cosmetic/UX discrepancy. No data is lost, no user flow is broken, and the affected cells are still visually distinct. Blast radius is limited to the Calendar screen's day cells.

---

## Spec Alignment

- **Type:** Spec gap — the spec explicitly prescribes borders for non-today cells and the code faithfully implements the spec. The user's desired behavior (background instead of border) is not described anywhere in the spec.
- **Violated constraint:** None — the code is compliant with the current spec constraint: *"Non-today cell border is 1.5dp; green (colorSuccess) when netAmount > 0, red (colorError) when netAmount < 0, no border when netAmount == null or == 0"* (`Calendar.shadow.md`, Constraints section, Calendar Grid).
- **Degraded success metric:** None formally defined, but degrades visual consistency — the "today" cell uses background color as the net indicator, while non-today cells use a border. The user wants a unified language (background only; border = today only).

---

## Root Cause Analysis

`CalendarDayCell.kt` has two separate visual channels for net amount:

1. **Background** — only set for `isToday` (colorSuccess/colorError @ 20%) or `isSelected` (bgDark). All other cells default to `Color.Transparent`.
2. **Border** — set to colorSuccess/colorError based on `netAmount` for all non-today cells; set to `bgDark` for today.

The `.border()` modifier at line 72 is always applied regardless of cell state, meaning non-today cells with a non-zero net amount always show a colored border:

```kotlin
// CalendarDayCell.kt:58-67
val borderColor = when {
    day.isToday -> colors.bgDark
    else -> day.dailySummary?.netAmount?.let { amount ->
        when {
            amount > 0L -> colors.colorSuccess  // ← produces green border on April 2
            amount < 0L -> colors.colorError
            else -> Color.Transparent
        }
    } ?: Color.Transparent
}

// CalendarDayCell.kt:72
.border(width = 1.5.dp, color = borderColor, shape = cellShape)
```

The background for non-today, non-selected cells is hardcoded to `Color.Transparent` at line 49:

```kotlin
// CalendarDayCell.kt:45-50
val background = when {
    day.isSelected -> selectedBackground
    day.isToday && (day.dailySummary?.netAmount ?: 0L) < 0L -> colors.colorError.copy(alpha = 0.2f)
    day.isToday -> todayBackground
    else -> baseBackground  // ← Color.Transparent — no background for non-today cells
}
```

The fix requires changing the visual language so that:
- Non-today, positive netAmount → colorSuccess @ 20% alpha **background**, no border
- Non-today, negative netAmount → colorError @ 20% alpha **background**, no border
- Today → bgDark border + net-responsive background (unchanged)
- Selected (non-today) → bgDark background (unchanged)

---

## Affected Files

| File | Lines |
|------|-------|
| `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt` | 45–50 (background logic), 58–67 (borderColor logic), 72 (border application) |
| `specs/shadow/Calendar.shadow.md` | Constraints section — "Non-today cell border" rule; CalendarDayCell Props section — "Border (non-today)" rule |

---

## Proposed Solution

**Code change (implementation error once spec is updated):**
1. Move net-amount signaling for non-today cells from `borderColor` to `background`: add a branch to the `background` `when` block for non-today, non-selected cells with `netAmount != null && netAmount != 0L`.
2. For non-today cells, set `borderColor` to `Color.Transparent` unconditionally (border is only needed for `isToday`).

**Spec update required:** Yes — update `specs/shadow/Calendar.shadow.md`:
- In the Constraints section, replace the "Non-today cell border" rule with: *"Non-today cell background is colorSuccess @ 20% when netAmount > 0, colorError @ 20% when netAmount < 0, transparent when null/0. Non-today cells never show a colored border."*
- Update the CalendarDayCell Props block to match.

Run `/spec` to update the shadow spec before or alongside `/bug-fix`.
