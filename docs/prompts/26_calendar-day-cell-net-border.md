# 26 — Calendar Day Cell: Net Sign Border

**Feature:** Calendar Day Cell visual simplification — replace net amount text with a colored border.
**Shadow Spec:** `specs/shadow/Calendar.shadow.md`

---

## Section 1 — Non-Functional Requirements

- **Rendering:** Border must be visible on all three background states: transparent (default), colorSuccess@20% (today), bgDark (selected). No additional computation beyond what `DaySummary.netAmount` already provides.
- **Correctness:** Border color must derive exclusively from `netAmount` — never from `totalIncome` or `totalExpenses` in isolation.
- **Accessibility:** The border alone may not be sufficient for color-blind users; state dots (pending/overdue/confirmed) remain present to convey transaction activity.
- **Performance:** Change is purely presentational — no new data fetching, repository calls, or use case changes required. Zero runtime overhead beyond a single `BorderStroke` object per non-empty cell.
- **Consistency:** `DaySummary.netAmount` field stays in the model (still needed for future use and the selected-day transaction list); only the rendering changes.

---

## Section 2 — Success Metrics

- **Functional:** Calendar day cells render a green border when `netAmount > 0`, red border when `netAmount < 0`, and no border when `netAmount == null || netAmount == 0L` — across all cell background states.
- **Visual correctness:** Border is 1.5dp, uses `BudgetCalendarTheme.colors.colorSuccess` / `colorError` tokens — no raw color values.
- **Regression-free:** State dots (amber pending, red overdue, green confirmed checkmark) continue to render correctly alongside the border.
- **No dead code:** The `netText` / `netColor` variables and the amount `Text` composable are fully removed — no commented-out code left behind.

---

## Section 3 — Risks and Assumptions

### Assumptions

- `DaySummary.netAmount` is already correctly computed (income − expenses) by `CalculateDaySummaryUseCase` — no changes needed there.
- `BudgetCalendarTheme.colors.colorSuccess` and `colorError` tokens are defined and available in `CalendarDayCell.kt`'s scope.
- `RoundedCornerShape(radius.lg)` used for clip is also the correct shape to pass to `.border()` so the border follows the cell's rounded corners.

### Risks

- **Border vs. selected background contrast:** bgDark is near-black. A 1.5dp green/red border should be visible, but it's worth a visual check on device — increase to 2dp if hard to see at a glance.
- **Today cell ambiguity:** The today background is already colorSuccess@20%. A green border on a green-tinted cell is intentional per spec, but may look redundant. If user feedback surfaces this, consider a slightly thicker border for today cells only.

---

## Section 4 — Tasks

- [ ] Remove net amount text from `CalendarDayCell` and add colored border (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** `CalendarDayCell.kt` currently renders `netText` as a `Text` composable and derives `netColor` **When** the cell is recomposed **Then** the `netText`, `netColor`, and amount `Text` composable are deleted; a `.border(1.5.dp, borderColor, RoundedCornerShape(radius.lg))` modifier is added to the outer `Column`; `borderColor` is `colorSuccess` when `netAmount > 0`, `colorError` when `netAmount < 0`, and `Color.Transparent` when `null` or `0L`
  - **NFRs:** Use `BudgetCalendarTheme.colors` tokens only — no raw hex or Color literals. Remove all dead variables (`netText`, `netColor`, `netAmount`) after the refactor.

---

## Section 5 — Definition of Done

- [ ] All tasks above are implemented and committed
- [ ] Acceptance criteria pass for every task
- [ ] Border visible and correct color on: default cell, today cell (green tint bg), selected cell (bgDark bg)
- [ ] No net amount `Text` composable or related variables remain in `CalendarDayCell.kt`
- [ ] State dots and confirmed checkmark still render correctly
- [ ] No raw color values introduced — all tokens from `BudgetCalendarTheme.colors`
- [ ] Shadow spec already updated (`specs/shadow/Calendar.shadow.md`) — no further spec changes needed
