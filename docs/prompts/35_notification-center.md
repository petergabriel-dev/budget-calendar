# 35 — Notification Center

**Feature:** Notification Center
**Shadow Spec:** `specs/shadow/NotificationCenter.shadow.md`
**Status:** Planned

---

## Section 1 — Non-Functional Requirements

- **Performance:** `GenerateNotificationsUseCase` runs synchronously within the existing app-start hook — no additional coroutine launch is needed beyond what `MarkOverdueTransactionsUseCase` already uses. Badge count Flow (`GetUnreadNotificationCountUseCase`) must emit within one DB round-trip; no perceptible Home screen recomposition delay.
- **Performance:** Reactive STS subscription in `BudgetViewModel` already exists — the notification check piggybacks on that emission; no new polling loop.
- **Reliability:** All generation logic is idempotent — calling `GenerateNotificationsUseCase` N times must produce the same DB state as calling it once. This is enforced by the upsert pattern for OVERDUE/STS types and the (type × year × month) guard for rollover types.
- **Reliability:** Monetary payload fields (`projected_rollover`, `current_sts`, `pool_balance`) must be stored as `Long` cents — never `Double` or `Float`. This is consistent with all other monetary values in the schema.
- **Reliability:** `user_settings` row may not exist on first launch. All consumers of `GetUserSettingsUseCase` must handle a null result and fall back to the default threshold of 15%.
- **Data integrity:** `notifications.type` is constrained by a SQL `CHECK` — only the six defined type strings are valid. The SQLDelight schema enforces this at insert time.
- **Correctness:** `daysRemainingInMonth()` from `DateUtils` already counts today as a remaining day — rollover trigger logic must use this function, not a manual calculation.
- **Usability:** Badge count must reflect the count of non-dismissed notifications at all times, including immediately after a dismiss action (no stale state). The `NotificationCenterSheet` must show an empty state when all notifications are dismissed.
- **Accessibility:** Dismiss button (X) on each notification item must meet the 44×44dp minimum touch target. "Clear all" button must have a content description.

---

## Section 2 — Success Metrics

- Badge count on the bell icon is always equal to `COUNT(*) FROM notifications WHERE is_dismissed = 0` — verified by reading DB state after each generation and dismiss action in tests.
- `GenerateNotificationsUseCase` is idempotent: running it twice with the same inputs leaves the DB in the same state as running it once — verified in unit tests for each notification type.
- Rollover reminders are generated at most once per (type × year × month): dismissing a rollover and re-running generation does not re-insert for the same month — verified in unit tests.
- STS_LOW and STS_NEGATIVE can both be active simultaneously and are independently dismissible — verified end-to-end.
- Overdue notification `overdue_count` updates reactively when a new transaction becomes overdue mid-session without a full app restart — verified by integration test.
- Profile page saves and persists a custom threshold; subsequent STS generation uses the new value — verified via `UpdateUserSettingsUseCase` unit test.
- Notification Center sheet opens from the bell, shows all undismissed notifications, expands/collapses items, and per-item and clear-all dismissal work correctly — verified manually on device/emulator.

---

## Section 3 — Risks and Assumptions

### Assumptions

- The existing foreground-resume hook that calls `MarkOverdueTransactionsUseCase` is accessible and can be extended to also call `GenerateNotificationsUseCase` without architectural changes.
- `CalculateMonthProjectionUseCase` already returns the projected rollover amount as a `Long` (cents) and is synchronously callable from within `GenerateNotificationsUseCase` (or its result is passed in as a parameter).
- `CalculateSafeToSpendUseCase` already emits via a Flow in `BudgetViewModel` — tapping into that existing emission for reactive STS notification generation requires no new Flow wiring.
- The app currently has no `user_settings` table or `Profile` screen infrastructure. A minimal profile screen (or a settings section in an existing screen) is sufficient for this feature — a full profile feature is out of scope here.
- `DateUtils.daysRemainingInMonth()` exists and returns an `Int` counting today as a remaining day (confirmed from `FEATURES.md` and prior work in the daily velocity feature).

### Risks

- **Rollover projection dependency:** `GenerateNotificationsUseCase` needs the projected rollover amount at launch time. If `CalculateMonthProjectionUseCase` is a Flow (not a suspend function), it cannot be called synchronously within the launch hook. Mitigation: call `.first()` on the Flow with a timeout, or restructure to accept the value as a parameter passed from the ViewModel.
- **Stale overdue badge:** If `MarkOverdueTransactionsUseCase` does not emit a signal after marking transactions overdue, the overdue notification update may not trigger mid-session. Mitigation: confirm that the existing `transactionChangedTrigger` / `balanceChangedTrigger` pattern (introduced in commit `c798c9f`) is used, and hook the overdue notification update into the same trigger.
- **Profile page doesn't exist:** If there is no existing profile/settings screen, creating one is a non-trivial UI task. Mitigation: assess during Task 12 whether the threshold setting can be placed in an existing screen (e.g., Account screen overflow menu) as a temporary home, deferring a full profile screen to a separate prompt.
- **SQLDelight migration numbering:** The current migration version is unknown without inspecting `.sqm` files. The wrong migration number will cause a runtime crash. Mitigation: check the latest `.sqm` file number before writing the migration (done in Task 1).

---

## Section 4 — Tasks

### Phase 1 — Database

- [ ] **Add DB migration for `notifications` and `user_settings` tables** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Use `cocoindex-code` to search `"SQLDelight migration"` and locate the existing `.sqm` files to determine the correct next migration number.
  - Create the next numbered `.sqm` file in `src/mobile/composeApp/src/commonMain/sqldelight/` with the full `CREATE TABLE` and `CREATE INDEX` statements for both `notifications` and `user_settings` as defined in the shadow spec Database Schema section.
  - **Given** the migration file is created with the correct version number **When** the app launches **Then** SQLDelight applies the migration without error and both tables are queryable.
  - **NFRs:** `notifications.type` must have the SQL `CHECK` constraint. `user_settings.id` must default to 1. All monetary columns are `INTEGER` (cents).

- [ ] **Add SQLDelight query files for `notifications` and `user_settings`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `Notifications.sq` and `UserSettings.sq` in the SQLDelight source directory.
  - Implement all queries from the shadow spec SQLDelight Queries section: `getActiveNotifications`, `getUnreadCount`, `getNotificationByType`, `getNotificationByTypeAndMonth`, `insertNotification`, `updateNotificationPayload`, `dismissNotification`, `dismissAllNotifications`, `getUserSettings`, `upsertUserSettings`.
  - **Given** the `.sq` files are created **When** Gradle syncs **Then** SQLDelight generates type-safe Kotlin query interfaces with no compilation errors.
  - **NFRs:** See Section 1.

---

### Phase 2 — Domain Layer

- [ ] **Create `NotificationType` enum and `Notification` domain model** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `features/notifications/domain/model/NotificationType.kt` with the six enum values: `OVERDUE_TRANSACTIONS`, `ROLLOVER_REMINDER_7`, `ROLLOVER_REMINDER_5`, `ROLLOVER_REMINDER_3`, `STS_LOW`, `STS_NEGATIVE`.
  - Create `features/notifications/domain/model/Notification.kt` with all fields from the Data Model section of the shadow spec.
  - Create `features/notifications/domain/model/UserSettings.kt`.
  - **Given** models exist **When** imported by use cases **Then** no compilation errors; all fields match the shadow spec Data Model exactly.
  - **NFRs:** `Notification.currentSts`, `poolBalance`, `projectedRollover` are `Long?` (cents). No `Double` or `Float`.

- [ ] **Create `INotificationRepository` and `IUserSettingsRepository` interfaces** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `features/notifications/domain/repository/INotificationRepository.kt` exposing: `getActiveNotifications(): Flow<List<Notification>>`, `getUnreadCount(): Flow<Int>`, `getByType(type: NotificationType): Notification?`, `getByTypeAndMonth(type: NotificationType, year: Int, month: Int): Notification?`, `insert(notification: Notification): Notification`, `updatePayload(id: Long, ...)`, `dismiss(id: Long)`, `dismissAll()`.
  - Create `features/notifications/domain/repository/IUserSettingsRepository.kt` exposing: `getUserSettings(): Flow<UserSettings?>`, `upsert(thresholdPercent: Int)`.
  - **Given** interfaces exist **When** referenced by use cases **Then** compile cleanly with no implementation details leaked.
  - **NFRs:** See Section 1.

- [ ] **Implement notification use cases** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create the following use cases in `features/notifications/domain/usecase/`:
    - `GenerateNotificationsUseCase` — accepts overdue transactions list, days remaining, projected rollover, current STS, pool balance, and threshold; implements deduplication logic per the Constraints section; calls repository upsert/insert as appropriate.
    - `GetActiveNotificationsUseCase` — delegates to `INotificationRepository.getActiveNotifications()`.
    - `GetUnreadNotificationCountUseCase` — delegates to `INotificationRepository.getUnreadCount()`.
    - `DismissNotificationUseCase` — calls `INotificationRepository.dismiss(id)`.
    - `DismissAllNotificationsUseCase` — calls `INotificationRepository.dismissAll()`.
  - **Given** `GenerateNotificationsUseCase` is called twice with identical inputs **When** the DB is inspected **Then** the row count for each type is unchanged (idempotent). **Given** a rollover notification exists for (type, year, month) **When** `GenerateNotificationsUseCase` runs again for the same month **Then** no new row is inserted.
  - **NFRs:** All deduplication logic is pure Kotlin — no side effects beyond repository calls.

- [ ] **Implement user settings use cases** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `GetUserSettingsUseCase` — returns `Flow<UserSettings?>`; callers fall back to threshold = 15 on null.
  - Create `UpdateUserSettingsUseCase` — validates `thresholdPercent` is in [1, 100], then calls `IUserSettingsRepository.upsert()`.
  - **Given** no `user_settings` row exists **When** `GetUserSettingsUseCase` emits **Then** it emits `null` (not a crash). **Given** `UpdateUserSettingsUseCase` is called with 0 or 101 **When** it executes **Then** it returns a validation error without writing to DB.
  - **NFRs:** See Section 1.

---

### Phase 3 — Data Layer

- [ ] **Implement `NotificationMapper`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `features/notifications/data/mapper/NotificationMapper.kt` mapping between the SQLDelight-generated DB entity and the `Notification` domain model.
  - Map `type` TEXT ↔ `NotificationType` enum (use `NotificationType.valueOf()` / `.name`).
  - **Given** a DB row with all nullable payload fields null **When** mapped **Then** domain model fields are null (not zero or empty string).
  - **NFRs:** See Section 1.

- [ ] **Implement `NotificationRepositoryImpl` and `UserSettingsRepositoryImpl`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `features/notifications/data/repository/NotificationRepositoryImpl.kt` implementing `INotificationRepository` using the SQLDelight queries from Phase 1.
  - `getActiveNotifications()` and `getUnreadCount()` must return `Flow` using SQLDelight's `.asFlow().mapToList()` / `.mapToOne()` patterns (consistent with existing repository implementations — use `cocoindex-code` to search `"asFlow mapToList"` to find the pattern in use).
  - Create `features/notifications/data/repository/UserSettingsRepositoryImpl.kt`.
  - **Given** `dismissAll()` is called **When** `getUnreadCount()` emits next **Then** it emits `0`.
  - **NFRs:** See Section 1.

---

### Phase 4 — Dependency Injection

- [ ] **Wire notifications feature into `KoinModules.kt`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Use `cocoindex-code` to search `"KoinModules"` and locate the current `databaseModule`, `repositoryModule`, `useCaseModule`, and `viewModelModule` definitions.
  - Add `NotificationRepositoryImpl` and `UserSettingsRepositoryImpl` to `repositoryModule` (as `single<INotificationRepository>` and `single<IUserSettingsRepository>`).
  - Add all notification use cases to `useCaseModule` (as `factory { }`).
  - Add `NotificationCenterViewModel` to `viewModelModule` (as `viewModel { }`).
  - **Given** Koin modules are updated **When** the app launches **Then** no `NoBeanDefFoundException` for any notification type.
  - **NFRs:** See Section 1.

---

### Phase 5 — Generation Integration

- [ ] **Wire `GenerateNotificationsUseCase` into the app-start / foreground-resume hook** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Use `cocoindex-code` to search `"MarkOverdueTransactionsUseCase"` and locate where it is called at app launch and foreground resume.
  - After `MarkOverdueTransactionsUseCase.execute()` completes, call `GenerateNotificationsUseCase` passing: the current overdue transaction list, `DateUtils.daysRemainingInMonth()`, the projected rollover from `CalculateMonthProjectionUseCase` (call `.first()` if it's a Flow), the current STS from `CalculateSafeToSpendUseCase`, the pool balance, and the user's threshold (from `GetUserSettingsUseCase`, defaulting to 15 on null).
  - **Given** the app starts with 2 overdue transactions **When** the hook runs **Then** an `OVERDUE_TRANSACTIONS` notification exists in DB with `overdue_count = 2` and `is_dismissed = 0`.
  - **NFRs:** This entire operation runs in the existing coroutine scope — no new `GlobalScope` or `launch` calls.

- [ ] **Wire reactive overdue update after `MarkOverdueTransactionsUseCase`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Use `cocoindex-code` to search `"transactionChangedTrigger"` or `"balanceChangedTrigger"` to locate the existing reactive trigger pattern.
  - After the overdue marking Flow emits, call `GenerateNotificationsUseCase.checkOverdueNotification()` (or re-call the full use case) so the existing `OVERDUE_TRANSACTIONS` notification's `overdue_count` is updated without inserting a duplicate.
  - **Given** a user confirms one of 3 overdue transactions mid-session **When** `MarkOverdueTransactionsUseCase` re-runs **Then** the `OVERDUE_TRANSACTIONS` notification's `overdue_count` drops to 2 (UPDATE, not INSERT).
  - **NFRs:** See Section 1.

- [ ] **Wire reactive STS generation in `BudgetViewModel`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Use `cocoindex-code` to search `"CalculateSafeToSpendUseCase"` in `BudgetViewModel` to locate the existing STS Flow subscription.
  - Within the existing STS Flow `collect` / `flatMapLatest`, after updating `BudgetUiState`, call `GenerateNotificationsUseCase` with the new STS value, current pool balance, and user threshold.
  - This must handle both STS_LOW and STS_NEGATIVE independently in the same call.
  - **Given** STS drops below 15% of pool balance mid-session **When** the STS Flow emits **Then** an `STS_LOW` notification is upserted. **Given** STS was already below threshold **When** STS drops further **Then** the existing `STS_LOW` notification is UPDATED (not duplicated).
  - **NFRs:** See Section 1.

---

### Phase 6 — Presentation Layer

- [ ] **Create `NotificationCenterUiState` and `NotificationCenterViewModel`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `features/notifications/presentation/NotificationCenterUiState.kt` with fields: `notifications: List<Notification>`, `isLoading: Boolean`, `error: String?`, `expandedNotificationId: Long?`.
  - Create `features/notifications/presentation/NotificationCenterViewModel.kt` consuming `GetActiveNotificationsUseCase`, `DismissNotificationUseCase`, `DismissAllNotificationsUseCase`.
  - Expose a `unreadCount: Flow<Int>` from `GetUnreadNotificationCountUseCase` for the badge.
  - Handle `toggleExpand(id: Long)` — sets `expandedNotificationId` to the id if collapsed, or null if already expanded (accordion).
  - **Given** ViewModel is created **When** `GetActiveNotificationsUseCase` emits **Then** `uiState.notifications` reflects the current non-dismissed list. **Given** `dismissAll()` is called **When** the Flow re-emits **Then** `uiState.notifications` is empty.
  - **NFRs:** ViewModel uses `viewModelScope`. No raw coroutine launches.

- [ ] **Build `NotificationCenterSheet` composable** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Before implementing: call `mcp__pencil__batch_get` with node IDs `JxzdM` (Accordion Item), `MO989` (Button / Ghost), `b4SK9` (Badge / Error), `20TKu` (Alert / Error), `GdTRQ` (Alert / Warning), `ywlyv` (Badge / Overdue) from `specs/DESIGN_SYSTEM.pen` to read the authoritative visual spec for each.
  - Create `features/notifications/presentation/NotificationCenterSheet.kt` as a `ModalBottomSheet`.
  - Sheet header: "Notifications" title (font-section, 24pt bold, text-primary) + "Clear all" Ghost Button (MO989) right-aligned. Show "Clear all" only when `notifications` is non-empty.
  - Notification list: use `LazyColumn`. Each item is an `AccordionItem` (JxzdM) — collapsed shows type-specific title + relative timestamp (e.g. "2 hours ago"); expanded shows detail per type:
    - `OVERDUE_TRANSACTIONS`: list of overdue transactions from `GetActiveNotificationsUseCase` detail (fetched live); each row uses TransactionItem (Ks9uf) pattern with name + amount.
    - `ROLLOVER_REMINDER_*`: body text "Projected rollover: ₱{projectedRollover} · {daysRemaining} days left".
    - `STS_LOW`: body text "₱{currentSts} remaining · {percent}% of pool". Styled with Alert / Warning (GdTRQ) colors.
    - `STS_NEGATIVE`: body text "₱{currentSts} below zero". Styled with Alert / Error (20TKu) colors.
  - Per-item dismiss: X icon button (44×44dp) on the trailing edge of each collapsed item.
  - Empty state: centred "No new notifications" in text-tertiary, font-body-medium.
  - After implementing: re-read nodes `JxzdM`, `MO989` from `specs/DESIGN_SYSTEM.pen` and confirm visual fidelity before marking done.
  - **Given** 0 notifications **When** sheet opens **Then** empty state is shown. **Given** 3 notifications **When** user taps X on one **Then** list updates to 2 immediately.
  - **NFRs:** All spacing, radius, and color values must use `BcSpacing`, `BcColors` tokens — no raw dp/hex.

- [ ] **Add badge to bell icon and wire sheet open in `HomeScreen`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Use `cocoindex-code` to search `"actionIcon = Icons.Outlined.Notifications"` to locate the bell in `HomeScreen.kt` (currently at line ~145 with empty `onActionClick = {}`).
  - Observe `NotificationCenterViewModel.unreadCount` in the Home screen's composition.
  - Render a Badge / Error (b4SK9) count badge over the bell icon when `unreadCount > 0`. Badge must show the numeric count (cap display at "9+" if count > 9).
  - Wire `onActionClick` to set a `showNotificationCenter: Boolean` state to `true`, which renders `NotificationCenterSheet`.
  - Pass `NotificationCenterViewModel` into the sheet.
  - Before implementing badge: read node `b4SK9` from `specs/DESIGN_SYSTEM.pen` for exact badge styling. After implementing, re-read and confirm fidelity.
  - **Given** 2 undismissed notifications exist **When** Home screen renders **Then** bell shows badge with "2". **Given** user dismisses all from sheet **When** sheet closes **Then** badge is gone.
  - **NFRs:** Badge must not obscure the bell touch target (overlay, not replace). Minimum touch target for bell remains 44×44dp.

---

### Phase 7 — Profile Page Setting

- [ ] **Add STS low threshold setting to profile / settings surface** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Use `cocoindex-code` to search `"ProfileScreen"` or `"MeScreen"` or `"SettingsScreen"` to determine if a profile/settings screen exists. If it does, add the setting there. If not, assess whether a minimal settings section can be added to an existing screen (e.g., a bottom sheet accessible from the tab bar "Me" tab).
  - Add a labeled numeric input (Number Input, `qpBrt`) for "STS Low Alert Threshold (%)" with range [1, 100] and default 15.
  - On save, call `UpdateUserSettingsUseCase` with the new value. Show a validation error if out of range.
  - Load the current value on open via `GetUserSettingsUseCase`.
  - **Given** no user_settings row exists **When** settings screen opens **Then** input shows "15". **Given** user enters "25" and saves **When** STS reactive check next runs **Then** STS_LOW fires at 25% of pool balance instead of 15%.
  - **NFRs:** Input must validate on save — do not persist invalid values. See Section 1.

---

### Phase 8 — Tests

- [ ] **Unit test `GenerateNotificationsUseCase`** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Create `FakeNotificationRepository` in `commonTest` implementing `INotificationRepository` with in-memory list.
  - Create `FakeUserSettingsRepository` in `commonTest`.
  - Test cases:
    - Overdue: first call inserts; second call with same overdues updates (no duplicate). Dismissing then calling again inserts fresh.
    - Rollover: generates at days 7, 5, 3; does not generate on day 6. Dismissing and re-running same month does not re-insert. Running in a new month inserts fresh.
    - STS_LOW: fires when `currentSts < poolBalance × 0.15`. Does not fire when STS == 0 and poolBalance == 0. Does not fire when STS is exactly at threshold.
    - STS_NEGATIVE: fires when `currentSts < 0`. Coexists with STS_LOW (both active simultaneously when STS < 0 and < 15%).
    - Idempotency: running with identical inputs twice leaves the same DB state.
  - **Given** all test cases pass **When** running `./gradlew :composeApp:testDebugUnitTest` **Then** zero failures.
  - **NFRs:** Tests use fake repositories only — no SQLDelight or Android dependencies.

- [ ] **Unit test `UpdateUserSettingsUseCase` validation** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Test: `thresholdPercent = 0` → error, no write. `thresholdPercent = 101` → error, no write. `thresholdPercent = 1` → success. `thresholdPercent = 100` → success. `thresholdPercent = 15` → success (default).
  - **Given** tests pass **When** running unit tests **Then** zero failures.
  - **NFRs:** See Section 1.

- [ ] **Integration test: overdue notification reactive update** (Use Skill: kmp) (Reference: specs/shadow/NotificationCenter.shadow.md)
  - Add a test case to `BudgetFlowObservationIntegrationTest` (or a new `NotificationFlowIntegrationTest`) verifying that after a transaction transitions to overdue mid-session, the `OVERDUE_TRANSACTIONS` notification's `overdue_count` is updated (not duplicated) in the same DB.
  - **Given** one `OVERDUE_TRANSACTIONS` notification exists with `overdue_count = 1` **When** a second transaction transitions to overdue and the overdue check re-runs **Then** `overdue_count = 2` and row count for `OVERDUE_TRANSACTIONS` type remains 1.
  - **NFRs:** See Section 1.

---

## Section 5 — Definition of Done

- [ ] All tasks above are implemented and committed
- [ ] Acceptance criteria pass for every task
- [ ] Badge count is accurate after each generation, dismiss, and dismiss-all action
- [ ] `GenerateNotificationsUseCase` is idempotent — verified by unit test
- [ ] Rollover reminders generate exactly once per (type × month) — verified by unit test
- [ ] STS_LOW and STS_NEGATIVE coexist independently — verified by unit test
- [ ] `NotificationCenterSheet` visual fidelity confirmed against Pencil nodes `JxzdM`, `MO989`, `b4SK9` before and after implementation
- [ ] No raw hex, dp, or sp values in any new composable — all tokens from `BudgetCalendarTheme`
- [ ] `./gradlew :composeApp:testDebugUnitTest` passes with zero failures
- [ ] No critical or high-severity bugs open
- [ ] Shadow spec updated if implementation deviated from original design
