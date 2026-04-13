# 27 — Calendar Sticky Layout

Restructure `CalendarScreen` so the calendar section (header, projection pill, grid, day section header) is always visible, while the transaction list below is the sole scrollable region filling remaining space. Also removes the redundant `+ Add` FAB.

---

## Section 1 — Non-Functional Requirements

- **Correctness:** Horizontal swipe for month navigation must work whether the user swipes over the calendar or the transaction list area.
- **Layout stability:** The calendar section must never scroll or shift when the transaction list is scrolled.
- **Scroll behaviour:** The transaction list must scroll freely with no artificial height cap; items must not be clipped.
- **No regression:** All existing interactions (day tap, transaction tap/long-press, month nav arrows, delete dialog, transaction form sheet) must continue to work unchanged.
- **Usability:** The transition between sticky and scrollable regions must feel seamless — no visible separator or jump.

---

## Section 2 — Success Metrics

- [ ] Calendar header, projection pill, grid, and day section header remain on screen at all times while the transaction list is scrolled.
- [ ] Swiping left/right anywhere on screen (calendar or transaction list) triggers month navigation.
- [ ] Transaction list expands to fill all remaining vertical space below the calendar section; items are not clipped at 320 dp.
- [ ] `+ Add` FAB is absent from the screen; no orphaned imports or references remain.
- [ ] All existing interactions (tap, long-press, delete, edit, form sheet) pass manual smoke test.

---

## Section 3 — Risks and Assumptions

### Assumptions
- `DayTransactionList` already accepts an external `modifier`, so passing `Modifier.weight(1f)` from the caller is sufficient to drive its height.
- The `LazyColumn` inside `DayTransactionList` using `Modifier.weight(1f)` (replacing `heightIn(max = 320.dp)`) will correctly fill the Column's remaining space when the parent Column is itself height-constrained by `weight(1f)`.
- Removing `verticalScroll` from the calendar section will not break the horizontal drag gesture, which only reads horizontal delta.

### Risks
- **Nested scroll conflict:** A `LazyColumn` inside a `Column` with `pointerInput(detectHorizontalDragGestures)` could intercept vertical flings. Verify that vertical scroll inside the transaction list is not blocked by the outer gesture detector. `detectHorizontalDragGestures` only consumes horizontal pointer events, so this should be safe — but confirm at runtime.
- **"View all" button truncation:** The `TextButton` that appears when transactions > 20 sits below the `LazyColumn` inside the same Column. With `weight(1f)` on the `LazyColumn`, the button may be pushed out of view. If so, the TextButton should be placed inside the `LazyColumn` as a footer item rather than a sibling.

---

## Section 4 — Tasks

- [ ] **Restructure `CalendarScreen` layout** (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** `CalendarScreen` uses a single `verticalScroll` Column containing all content **When** refactored **Then** the layout is a root `Column(fillMaxSize)` with `pointerInput(detectHorizontalDragGestures)`, containing: (a) a non-scrollable sticky `Column` with `MonthNavigationHeader`, `EndOfMonthProjectionPill`, `CalendarGrid`, and `DayTransactionSectionHeader`; and (b) `DayTransactionList` with `Modifier.weight(1f)` filling remaining space.
  - Move `pointerInput(detectHorizontalDragGestures)` from the inner scrollable Column to the root `Column` so swipes over the transaction list also trigger month navigation.
  - Remove `verticalScroll(rememberScrollState())` and its import.
  - Remove the `Spacer(96.dp)` bottom padding that existed to prevent FAB overlap.
  - **NFRs:** See Section 1

- [ ] **Remove `+ Add` FAB from `CalendarScreen`** (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** a `BcButton` with text "Add" is rendered at `Alignment.BottomEnd` of the root `Box` **When** removed **Then** neither the button nor its `onClick` lambda remain; remove the outer `Box` wrapper if it is no longer needed (replace with `Column` as root); remove unused imports (`BcButton`, `ButtonVariant`).
  - **NFRs:** No orphaned state or imports.

- [ ] **Remove 320 dp height cap from `DayTransactionList`** (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
  - **Given** `LazyColumn` inside `DayTransactionList` has `.heightIn(max = 320.dp)` **When** the caller passes `Modifier.weight(1f)` **Then** replace `heightIn(max = 320.dp)` with `Modifier.weight(1f).fillMaxWidth()` on the `LazyColumn`, and ensure the outer `Column` in `DayTransactionList` uses the passed `modifier` so the weight is respected.
  - Verify the "View all" `TextButton` (transactions > 20 path) is still visible. If it is pushed out of view by the weighted `LazyColumn`, move it into the `LazyColumn` as a `footer` item.
  - Remove the `heightIn` import if it becomes unused.
  - **NFRs:** Transaction list must scroll freely to all items with no clipping.

---

## Section 5 — Definition of Done

- [ ] All tasks above are implemented and committed
- [ ] Calendar section is visually sticky — confirmed by scrolling the transaction list manually
- [ ] Horizontal swipe triggers month navigation from both the calendar area and the transaction list area
- [ ] Transaction list fills remaining screen space with no height cap
- [ ] `+ Add` FAB is gone; no compile warnings about unused imports
- [ ] No critical or high-severity bugs open
- [ ] CI/CD pipeline passing (build, lint, tests)
- [ ] Shadow spec updated if implementation deviated from original design
