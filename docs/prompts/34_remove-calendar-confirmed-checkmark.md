# 34 — Remove Calendar Day Confirmed Checkmark

> Remove the green "✓" text indicator from `CalendarDayCell`. The tinted background already communicates that a day has confirmed transactions; the checkmark is redundant visual noise.

**Shadow Spec:** `specs/shadow/Calendar.shadow.md`
**Feature:** Calendar View

---

## Section 1 — Non-Functional Requirements

- **Performance:** No impact — this is a pure UI deletion with no added computation.
- **Reliability:** Pending and overdue `StateDot` indicators must remain fully functional after the change.
- **Usability:** The tinted cell background (colorSuccess @ 20% for net positive, colorError @ 20% for net negative) must continue to serve as the sole confirmed-transaction signal — no regression in visual clarity.

---

## Section 2 — Success Metrics

- The confirmed checkmark no longer appears on any calendar day cell.
- Pending (amber dot) and overdue (red dot) indicators are unaffected.
- Cell background tinting continues to render correctly for all net-amount states.
- No new compiler warnings or errors introduced.

---

## Section 3 — Risks and Assumptions

**Assumptions**
- The only confirmed indicator is the `"✓"` `Text` composable inside the `Row` in `CalendarDayCell.kt` — no other call site renders this indicator.
- `hasConfirmed` in `DaySummary` is still used by other code paths (e.g., tests or future features) and should not be removed from the data model — only the UI render call is deleted.

**Risks**
- None. This is a single-block deletion with no domain or data layer changes.

---

## Section 4 — Tasks

- [ ] Remove the confirmed checkmark block from `CalendarDayCell` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** `CalendarDayCell.kt` renders a `"✓"` `Text` when `day.dailySummary?.hasConfirmed == true` **When** that block is deleted **Then** no checkmark appears on any day cell, and the amber/red `StateDot` indicators are unaffected
  - **Implementation:** Delete lines 102–109 of `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt` — the entire `if (day.dailySummary?.hasConfirmed == true) { Text(...) }` block inside the state `Row`.
  - **NFRs:** See Section 1

---

## Section 5 — Definition of Done

- [ ] Confirmed checkmark block deleted from `CalendarDayCell.kt`
- [ ] Pending and overdue dots still render correctly
- [ ] Background tinting unchanged for positive, negative, and empty day states
- [ ] Project builds without errors or warnings
- [ ] Shadow spec (`specs/shadow/Calendar.shadow.md`) already updated — no further spec changes needed
