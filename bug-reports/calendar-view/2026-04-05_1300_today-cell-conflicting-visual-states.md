# Bug Report: Today Cell — Conflicting Border and Background Visual States

**Feature:** Calendar View
**Date:** 2026-04-05
**Reported by:** User observation (screenshot)

---

## Description

When today's date has a negative `netAmount` (expenses exceed income), the calendar day cell renders simultaneously with:
- A **red border** (driven by the net-amount border logic)
- A **green background** (driven by the `isToday` highlight)

These two signals directly contradict each other. A user glancing at the calendar sees the green tint and interprets "today is positive", while the red border signals "today is negative". The correct behavior is: today's border should always be black (`bgDark`) as the "today" identifier, and the background should communicate the net amount (red if negative, green otherwise).

**Reproduction steps:**
1. Ensure today's date has at least one expense with no offsetting income (negative `netAmount`).
2. Open the Calendar screen.
3. Observe today's cell: red border + green background.

**Affected persona:** Any user viewing the Calendar screen on a day where expenses exceed income.

---

## Severity

**Low** — This is a pure visual conflict with no functional impact; the correct transactions still load when the day is tapped, and no data is corrupted. However, it produces a directly misleading financial signal (green = "you're positive" when you're not), which undermines the design tenet of "Financial Visibility".

---

## Spec Alignment

- **Type:** Implementation error — the code diverged from the intended design. The shadow spec now explicitly defines the correct behavior (updated 2026-04-05).
- **Violated constraint:** `specs/shadow/Calendar.shadow.md` — Calendar Grid section:
  > "Today cell border is always bgDark (black, 1.5dp) regardless of netAmount — the dark border is the 'today' indicator"
  > "Today cell background reflects net amount: colorError @ 20% alpha when netAmount < 0, colorSuccess @ 20% alpha otherwise"
  > (These constraints were absent prior to the spec update — the original spec only said "current day is highlighted with colorSuccess @ 20% alpha" without addressing the net-amount conflict.)
- **Violated FEATURES.md rule:** CAL-006 (updated): "Current day always has a bgDark (black) border as the 'today' indicator; its background is colorError @ 20% when netAmount < 0, colorSuccess @ 20% otherwise"
- **Degraded success metric:** "Financial Visibility" design tenet — users cannot correctly read today's net financial position at a glance.

---

## Root Cause Analysis

In `CalendarDayCell.kt`, the `background` and `borderColor` values are computed independently with no shared awareness:

**Background (lines 45–49):**
```kotlin
val background = when {
    day.isSelected -> selectedBackground
    day.isToday -> todayBackground          // always colorSuccess @ 20% — ignores netAmount
    else -> baseBackground
}
```

**Border (lines 57–63):**
```kotlin
val borderColor = day.dailySummary?.netAmount?.let { amount ->
    when {
        amount > 0L -> colors.colorSuccess
        amount < 0L -> colors.colorError    // red border applied even on today
        else -> Color.Transparent
    }
} ?: Color.Transparent
```

The `isToday` check in `background` has no knowledge that `borderColor` will be red, and the `borderColor` logic has no special case for `isToday`. When `isToday == true` and `netAmount < 0`, both branches fire and produce conflicting signals.

---

## Affected Files

| File | Lines | Role |
|------|-------|------|
| `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt` | 45–49 (background), 57–63 (borderColor) | Root cause — both `background` and `borderColor` computed independently with no `isToday` × `netAmount` coordination |

No other files are involved — this is fully contained to the presentation layer.

---

## Proposed Solution

Two targeted changes to `CalendarDayCell.kt`:

1. **Background:** Add an `isToday && netAmount < 0` branch before the general `isToday` branch, mapping to `colorError.copy(alpha = 0.2f)`.

2. **Border:** Add an `isToday` guard at the top of the `borderColor` expression that returns `colors.bgDark` unconditionally, bypassing the net-amount logic entirely for today.

No domain, repository, or database changes required. No shadow spec update needed beyond what was already done (2026-04-05).

The fix plan is documented at: `docs/prompts/31_bugfix_calendar-day-today-cell-visual-state.md`
