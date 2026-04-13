# 31 — Bug Fix: Calendar Day Cell "Today" Visual State

**Feature:** Calendar View — `CalendarDayCell` visual state for today
**Shadow Spec:** `specs/shadow/Calendar.shadow.md`
**Scope:** Presentation layer only — one file, no DB or domain changes

---

## Problem

When today's date has a negative `netAmount` (more expenses than income), the day cell renders a **red border + green background** simultaneously. These two signals conflict: the red border comes from the net-amount logic and the green background comes from the `isToday` highlight. The result is visually incoherent and misleading.

**Correct behavior:**
- Today's cell **always** has a `bgDark` (black) border — this is the "today" indicator.
- Today's cell **background** communicates net amount: `colorError @ 20%` when negative, `colorSuccess @ 20%` otherwise.
- Non-today cells keep the current border-color-as-net-sign logic untouched.

---

## Section 1 — Non-Functional Requirements

- **Correctness:** The `isToday` and net-amount visual states must never produce conflicting signals on the same cell simultaneously.
- **Token compliance:** All colors must come from `BudgetCalendarTheme.colors` — no raw hex values (DS-001).
- **No regressions:** Non-today cells (selected, default, adjacent-month) must behave identically to before.
- **Accessibility:** The black border provides sufficient contrast against both the red and green background variants (WCAG 3:1 minimum for UI components).

---

## Section 2 — Success Metrics

- [ ] Today cell with negative `netAmount` renders a **black border** and **light red background** — no red border, no green background.
- [ ] Today cell with positive or zero `netAmount` renders a **black border** and **light green background** — unchanged from current positive-day behavior.
- [ ] Today cell with no transactions (`dailySummary == null`) renders a **black border** and **light green background**.
- [ ] Non-today cells with positive/negative net still show green/red borders with transparent background.
- [ ] Selected-day (non-today) cell still renders `bgDark` background and `textInverted` content.

---

## Section 3 — Risks and Assumptions

**Assumptions**
- `colors.bgDark` maps to `#000000` (verified in `DESIGN_SYSTEM.md` — `--bg-dark`).
- `colors.colorSuccess` and `colors.colorError` are available directly on `BudgetCalendarTheme.colors` (confirmed in existing code at line 41 of `CalendarDayCell.kt`).
- `isToday` and `isSelected` are mutually exclusive in practice (today is auto-selected on load, but once the user taps another day, today is no longer selected).

**Risks**
- If `isToday && isSelected` can both be true simultaneously, the `background` when-block priority (selected first, then today) means the today-net-responsive background is skipped. Verify whether the ViewModel ever sets `isSelected = true` on today after initial load, and if so, confirm the black border still applies correctly (it will, since the border logic is independent of `isSelected`).

---

## Section 4 — Tasks

- [ ] Fix `CalendarDayCell.kt` background and border logic for today state (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **File:** `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt`
  - **Change 1 — background (lines 45–49):** Replace the `isToday -> todayBackground` branch with two branches:
    ```kotlin
    val background = when {
        day.isSelected -> selectedBackground
        day.isToday && (day.dailySummary?.netAmount ?: 0L) < 0L -> colors.colorError.copy(alpha = 0.2f)
        day.isToday -> todayBackground   // colorSuccess @ 20% (unchanged token)
        else -> baseBackground
    }
    ```
  - **Change 2 — borderColor (lines 57–63):** Replace the net-amount-only logic with a today-first guard:
    ```kotlin
    val borderColor = when {
        day.isToday -> colors.bgDark
        else -> day.dailySummary?.netAmount?.let { amount ->
            when {
                amount > 0L -> colors.colorSuccess
                amount < 0L -> colors.colorError
                else -> Color.Transparent
            }
        } ?: Color.Transparent
    }
    ```
  - **Given** today has `netAmount < 0` **When** `CalendarDayCell` renders **Then** border is `bgDark` and background is `colorError @ 20%`
  - **Given** today has `netAmount > 0` **When** `CalendarDayCell` renders **Then** border is `bgDark` and background is `colorSuccess @ 20%`
  - **Given** today has no transactions (`dailySummary == null`) **When** `CalendarDayCell` renders **Then** border is `bgDark` and background is `colorSuccess @ 20%`
  - **Given** a non-today cell has `netAmount < 0` **When** `CalendarDayCell` renders **Then** border is `colorError` and background is transparent
  - **NFRs:** See Section 1

- [ ] Read component `AbKW9` (Day Wrapper / Active) and `sqaBa` (Day Wrapper / Default) from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get`, confirm the fix matches the design spec, correct any mismatches (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** the Pencil spec defines day cell visual states **When** the implemented Compose code is compared against it **Then** token usage, border width, and background alpha are consistent with the spec
  - **NFRs:** See Section 1

---

## Section 5 — Definition of Done

- [ ] All tasks above are implemented and committed
- [ ] Acceptance criteria pass for every task (manually verified on device/emulator)
- [ ] Today cell with negative net renders black border + light red background
- [ ] No regressions on non-today cells, selected cell, or adjacent-month cells
- [ ] No raw hex or raw dp values introduced — all tokens from `BudgetCalendarTheme`
- [ ] Shadow spec (`specs/shadow/Calendar.shadow.md`) matches implementation
