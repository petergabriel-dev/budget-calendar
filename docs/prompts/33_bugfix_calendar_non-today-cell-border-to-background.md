# Bug Fix: Calendar — Non-Today Cell Net-Amount Indicator (Border → Background)

> Reference: `bug-reports/calendar/2026-04-05_1200_non-today-cell-border-instead-of-background.md`
> Shadow Spec: `specs/shadow/Calendar.shadow.md`

---

## Section 1 — Bug Summary

**Root Cause:**
`CalendarDayCell.kt` uses two separate visual channels for net-amount signaling:
- **Border** carries the green/red net signal for all non-today cells (lines 58–67).
- **Background** is only set for `isToday` and `isSelected` cells; all other cells default to `Color.Transparent` (lines 45–50).

The spec has been updated (via `/spec`) to make background the sole net-amount channel for non-today cells and border the exclusive "today" indicator. The code has not yet been updated to match.

**Affected Layer(s):** Frontend only — `CalendarDayCell.kt` (Compose UI component).

**Severity:** Low — cosmetic/UX only; no data loss, no broken user flow.

---

## Section 2 — Non-Functional Requirements

- **Performance:** Recomposition cost must not increase — the change is a swap of modifier values, not structural; no new state or flows introduced.
- **Correctness:** All four cell states must remain visually correct after the fix:
  - Non-today, positive net → colorSuccess @ 20% background, no colored border
  - Non-today, negative net → colorError @ 20% background, no colored border
  - Non-today, zero/null net → transparent background, no border
  - Today → bgDark border + net-responsive background (unchanged)
  - Selected non-today → bgDark background (unchanged)
- **Reliability:** No silent fallback — the `when` branches must be exhaustive and cover the `isSelected` + non-zero net combination correctly (selected state wins over net tint).
- **Regression:** Existing passing tests for `CalendarDayCell` must continue to pass; today-cell visual tests must not regress.

---

## Section 3 — Risks and Assumptions

**Assumptions:**
- The fix is purely in `CalendarDayCell.kt` — no ViewModel, use case, or domain model changes needed.
- `CalendarDay.isSelected` and `CalendarDay.isToday` are mutually exclusive in practice for the common case (today is not also selected separately), but the code handles both simultaneously and the fix must preserve that.
- There are no other call sites that render a calendar day cell differently — `CalendarDayCell` is the single component for all grid cells.

**Risks:**
- **Selected + non-zero net:** When `isSelected == true` and `netAmount != 0`, the selected background (bgDark) must win over the net tint. If the `when` ordering is wrong, a selected day with income could show green instead of black.
- **Today + selected simultaneously:** The current code applies `selectedBackground` first (before the `isToday` check). This ordering must be preserved — if today is also the selected date, it should show `selectedBackground` (bgDark), not `todayBackground`. Verify the `when` branch order carefully.
- **Border modifier still present:** Simply setting `borderColor = Color.Transparent` keeps the `.border()` modifier in the layout tree (harmless but slightly wasteful). This is acceptable for now; removing the modifier entirely would also work but is a slightly larger change.

---

## Section 4 — Fix Tasks

- [ ] **Write a failing test that asserts non-today cells with positive netAmount render with a green background and no colored border** (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** a `CalendarDay` with `isToday = false`, `isSelected = false`, and `dailySummary.netAmount = 500L` **When** `CalendarDayCell` is rendered **Then** the cell background is `colorSuccess @ 20% alpha` and the border color is `Color.Transparent`
  - **NFRs:** Test must be in `commonMain` or `androidUnitTest`; use `CalendarDay` and `DaySummary` fakes directly — no DB or repository needed

- [ ] **Write a failing test that asserts non-today cells with negative netAmount render with a red background and no colored border** (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** a `CalendarDay` with `isToday = false`, `isSelected = false`, and `dailySummary.netAmount = -300L` **When** `CalendarDayCell` is rendered **Then** the cell background is `colorError @ 20% alpha` and the border color is `Color.Transparent`
  - **NFRs:** See Section 2

- [ ] **Fix `CalendarDayCell.kt`: move net-amount signaling from `borderColor` to `background` for non-today cells** (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

  Update the `background` `when` block (`CalendarDayCell.kt:45–50`) to add branches for non-today, non-selected cells with non-zero net amount, **before** the `else -> baseBackground` fallback:

  ```kotlin
  val background = when {
      day.isSelected -> selectedBackground
      day.isToday && (day.dailySummary?.netAmount ?: 0L) < 0L -> colors.colorError.copy(alpha = 0.2f)
      day.isToday -> todayBackground
      !day.isToday && (day.dailySummary?.netAmount ?: 0L) > 0L -> colors.colorSuccess.copy(alpha = 0.2f)
      !day.isToday && (day.dailySummary?.netAmount ?: 0L) < 0L -> colors.colorError.copy(alpha = 0.2f)
      else -> baseBackground
  }
  ```

  Update the `borderColor` `when` block (`CalendarDayCell.kt:58–67`) to remove the net-amount branches — only today gets a colored border:

  ```kotlin
  val borderColor = when {
      day.isToday -> colors.bgDark
      else -> Color.Transparent
  }
  ```

  - **Given** the fix is applied **When** any non-today, non-selected cell with `netAmount > 0` is rendered **Then** it shows a green tinted background and a transparent border
  - **Given** the fix is applied **When** any non-today, non-selected cell with `netAmount < 0` is rendered **Then** it shows a red tinted background and a transparent border
  - **Given** the fix is applied **When** today's cell is rendered **Then** it still shows the bgDark border and the net-responsive background (unchanged behavior)
  - **NFRs:** `when` branch order must keep `isSelected` first so selection always wins over net tint; no new state or recomposition triggers introduced

- [ ] **Verify the selected + non-zero net edge case visually** (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** `isSelected = true` and `netAmount = 500L` (or any non-zero value) **When** the cell is rendered **Then** the background is `bgDark` (selected wins), not the net tint — confirm the `when` ordering is correct
  - **NFRs:** This can be a unit assertion or a manual smoke test on device; document the result

- [ ] **Run all existing Calendar tests and confirm no regressions** (Use Skill: kmp)
  - **Given** the fix is applied **When** `./gradlew :composeApp:testDebugUnitTest --tests "com.petergabriel.budgetcalendar.features.calendar.*"` is run **Then** all previously passing tests pass
  - **NFRs:** Zero new test failures; build must be clean

---

## Section 5 — Definition of Done

- [ ] Reproduction tests (positive net → green bg, negative net → red bg) were failing before the fix and pass after
- [ ] All fix tasks implemented and committed
- [ ] Acceptance criteria pass for every task
- [ ] No NFR benchmarks regressed
- [ ] Today-cell visual behavior unchanged (bgDark border + net-responsive background)
- [ ] Selected-cell visual behavior unchanged (bgDark background, selection wins over net tint)
- [ ] All existing Calendar tests pass
- [ ] Shadow spec (`Calendar.shadow.md`) already updated via `/spec` — no further spec changes needed
- [ ] Bug report marked as resolved
