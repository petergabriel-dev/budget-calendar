# 28 — Calendar Day Transaction Expand

> Adds a chevron-up icon to `DayTransactionSectionHeader` that opens a full-screen `ModalBottomSheet` showing all transactions for the selected day. The calendar disappears behind the sheet; swiping down restores it.

**Shadow spec:** `specs/shadow/Calendar.shadow.md`
**Skill:** `kmp`

---

## Section 1 — Non-Functional Requirements

- **Performance:** Sheet open animation must complete in ≤ 300ms on mid-range Android hardware; transaction list must render without jank for up to 100 items.
- **Reliability:** The sheet must show the same transaction list as the inline list below the grid — no data re-fetch, no divergence. Data is passed from existing `uiState.selectedDayTransactions`.
- **Usability:** Chevron must have a minimum 44×44dp touch target (Material guidelines). Sheet must support fling-to-dismiss (default `ModalBottomSheet` behavior).
- **Correctness:** Money values displayed in the sheet must use `CurrencyUtils` — no raw cents shown. Timestamps must be rendered in device local timezone via `DateUtils`.
- **Design fidelity:** Section header must be read from `specs/DESIGN_SYSTEM.pen` (node `oMq1b`) via `mcp__pencil__batch_get` before implementation, and confirmed after.

---

## Section 2 — Success Metrics

- **Functional:** Tapping the chevron opens the sheet; swiping down closes it; the full transaction list for the selected day is shown with no truncation.
- **Data integrity:** The sheet's transaction list is identical to what `uiState.selectedDayTransactions` holds — verified by visual inspection and unit test.
- **Design fidelity:** Header text, font, size, and chevron icon placement match the Pencil design spec for `oMq1b`.
- **No regression:** Existing calendar interactions (day selection, month navigation, swipe gesture, transaction tap/long-press, edit sheet) continue to work correctly.

---

## Section 3 — Risks and Assumptions

### Assumptions
- `ModalBottomSheet` from Material3 (`androidx.compose.material3`) is already available — no new dependency required.
- `uiState.selectedDayTransactions` already contains the complete list for the selected day; no additional repository query is needed inside the sheet.
- The existing `onViewAllTransactions` callback in `DayTransactionList` can be removed since the chevron in the header replaces its purpose.

### Risks
- **ModalBottomSheet z-ordering with existing edit sheet:** `CalendarScreen` already uses a `ModalBottomSheet` for the transaction form. Two overlapping sheet states must be mutually exclusive — ensure only one sheet is shown at a time and state is reset correctly.
- **Drag gesture conflict:** `CalendarScreen` has a horizontal drag gesture for month navigation. A vertically-draggable bottom sheet sitting below the calendar must not interfere with the horizontal swipe. Verify gesture disambiguation on device.
- **Shadow spec gap:** `DayTransactionSectionHeaderProps` in the shadow spec has no `onExpand` callback. The spec must be updated as part of this task to stay authoritative.

---

## Section 4 — Tasks

- [ ] **Update shadow spec** — add `onExpand: () -> Unit` to `DayTransactionSectionHeaderProps` and document the expand sheet behavior in the Constraints section. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** `specs/shadow/Calendar.shadow.md` exists **When** the expand feature is added **Then** the spec reflects the new prop and sheet behavior so it remains the source of truth.
  - **NFRs:** See Section 1.

- [ ] **Read Pencil design for section header** — call `mcp__pencil__batch_get` with node `oMq1b` from `specs/DESIGN_SYSTEM.pen` to get exact token values (font, size, icon size, spacing) before writing any code. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** the section header uses design component `oMq1b` **When** starting implementation **Then** exact token values are known and no guessing occurs.
  - **NFRs:** Design fidelity — values from Pencil supersede `DESIGN_SYSTEM.md`.

- [ ] **Update `DayTransactionSectionHeader`** — add `onExpand: () -> Unit` parameter; render a chevron-up `Icon` (`Icons.Default.KeyboardArrowUp` or equivalent) right-aligned in the header row with a 44×44dp touch target. Remove any existing "View all" trigger if present. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** a day is selected **When** the section header is visible **Then** a chevron-up icon appears right-aligned; tapping it calls `onExpand`.
  - **NFRs:** 44×44dp minimum touch target; icon must respect design tokens from `oMq1b`.

- [ ] **Add expand sheet state to `CalendarScreen`** — introduce `var showExpandSheet by remember { mutableStateOf(false) }`. Pass `onExpand = { showExpandSheet = true }` to `DayTransactionSectionHeader`. Ensure this sheet and the existing transaction-form sheet are mutually exclusive (opening one closes the other). (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** the user taps the chevron **When** `onExpand` fires **Then** `showExpandSheet` becomes `true` and the expand sheet opens; the transaction-form sheet is not simultaneously visible.
  - **NFRs:** No duplicate sheet state; sheet dismisses on back gesture.

- [ ] **Implement expand `ModalBottomSheet` content** — when `showExpandSheet == true`, render a `ModalBottomSheet` with `onDismissRequest = { showExpandSheet = false }`. Content: day header (`DayTransactionSectionHeader` in non-interactive read-only mode or a plain `Text`), followed by a `LazyColumn` of all `uiState.selectedDayTransactions` using `TransactionListItem`. No truncation — show all items. Wire `onTransactionTap` and `onTransactionLongPress` to the existing handlers; tapping a transaction should dismiss the expand sheet before opening the edit sheet. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** the expand sheet is open **When** it renders **Then** all transactions for the selected day are shown without truncation; tap/long-press work identically to the inline list.
  - **NFRs:** Render ≤ 300ms; no data re-fetch; money formatted via `CurrencyUtils`; dates via `DateUtils`.

- [ ] **Remove `onViewAllTransactions` from `DayTransactionList`** — the chevron in the header replaces this. Remove the callback parameter, the "View all" button, and the 20-item cap. All items should now render directly in the inline list (scroll is sufficient for moderate counts). Update all call sites in `CalendarScreen`. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** `DayTransactionList` previously capped at 20 items **When** refactored **Then** all transactions render inline; no "View all" button appears; `onViewAllTransactions` parameter is gone.
  - **NFRs:** No regression on inline list behavior.

- [ ] **Pencil fidelity confirmation** — after implementing, call `mcp__pencil__batch_get` on node `oMq1b` again and diff the rendered header against the code. Correct any mismatches in font size, weight, icon size, or spacing before marking done. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** the implementation is complete **When** re-reading the Pencil spec **Then** all visual properties match; no deviations remain.
  - **NFRs:** Design fidelity — Pencil is source of truth.

- [ ] **Verify no gesture conflict on device** — manually test horizontal swipe for month navigation while the expand sheet is closed, and vertical sheet drag-to-dismiss while it is open. Confirm no accidental month navigation triggers when dismissing the sheet. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** the expand sheet is open **When** the user drags down to dismiss **Then** only the sheet dismisses; no month navigation fires.
  - **NFRs:** Gesture disambiguation must be correct on physical device or emulator.

---

## Section 5 — Definition of Done

- [ ] All tasks above are implemented and committed
- [ ] Acceptance criteria pass for every task
- [ ] Expand sheet opens and closes correctly; all transactions visible without truncation
- [ ] Chevron has correct 44×44dp touch target and matches Pencil design tokens
- [ ] No regression on: day selection, month navigation, swipe gesture, transaction edit sheet, long-press delete
- [ ] `DayTransactionList` no longer has 20-item cap or "View all" button
- [ ] Shadow spec updated with `onExpand` prop and sheet behavior
- [ ] Gesture conflict verified on device — no accidental month navigation on sheet dismiss
- [ ] CI/CD pipeline passing (build, lint, tests)
