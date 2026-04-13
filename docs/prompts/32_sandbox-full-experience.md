# Sandbox Mode — Full Multi-Transaction Planning Experience

> Upgrades sandbox from a standalone ephemeral screen to a persistent, reactive multi-transaction budget scratchpad integrated into the Home tab

**Shadow Spec:** specs/shadow/Sandbox.shadow.md
**Prerequisite:** Phase 6 (`07_phase-6-sandbox-mode.md`) — fully complete. All domain models, use cases, repository, DI, and standalone UI screens are built. This plan builds on that work.

---

## Section 1 — Non-Functional Requirements

- **Performance:** `projectedSafeToSpend` must recalculate and emit to the UI within 100ms of a sandbox transaction insert or delete. The reactive chain (DB write → SQLDelight Flow → UseCase → ViewModel `combine` → UI recomposition) must not block the main thread; all computation happens in `Dispatchers.IO` or `Dispatchers.Default`.
- **Reliability:** Sandbox transactions are fully persisted to SQLite. No sandbox data is lost on app kill, backgrounding, or navigation away from the home screen. Only `CheckAndExpireSandboxesUseCase` at app start can delete sandbox data.
- **Correctness:** `projectedSafeToSpend = initialSafeToSpend + Σ(income amounts) − Σ(expense amounts)`. This formula must be identical to how `GetSandboxSafeToSpendUseCase` computes it — no divergence between DB query and in-memory calculation.
- **Isolation:** Real STS queries always include `is_sandbox = 0`. Sandbox transactions must never appear in real balance or STS calculations at any point.
- **Usability:** All interactive elements (Add, Remove, Promote, segmented control) meet 44×44dp minimum touch target. `projectedSafeToSpend` shown in `colorError` when negative must also include a warning icon — color is not the sole signal. All loading states show a skeleton or spinner.
- **Scalability:** The reactive `combine` in the ViewModel must not leak coroutines. Flow collections scoped to `viewModelScope` are cancelled when `setSandboxMode(false)` is called or the ViewModel is cleared.

---

## Section 2 — Success Metrics

- **Functional:** User can create a sandbox, add multiple income and expense transactions, see `projectedSafeToSpend` update immediately after each addition without tapping any refresh button, and return to the same state after closing and reopening the app.
- **Functional:** Comparison delta (sandbox STS vs real STS) is visible inline on the sandbox home view and updates reactively when either sandbox transactions or real transactions change.
- **Functional:** Individual sandbox transactions can be promoted to real PENDING transactions; the sandbox list updates immediately after promotion.
- **Performance:** `projectedSafeToSpend` emits a new value within 100ms of a `insertSandboxTransaction` or `deleteSandboxTransaction` DB call on a mid-range device.
- **Quality:** All reactive use cases (`GetSandboxSafeToSpendUseCase`, `CompareSandboxWithRealityUseCase`) have unit tests asserting correct Flow emissions after add/remove operations. 100% pass rate on all sandbox use case tests.
- **Quality:** `FakeSandboxRepository` supports reactive `Flow` emissions so ViewModel tests can assert state transitions without a real DB.

---

## Section 3 — Risks and Assumptions

### Assumptions

- Phase 6 migration already added `initial_safe_to_spend INTEGER NOT NULL DEFAULT 0` and `description TEXT` to `sandbox_snapshots`. Tasks in this plan verify the schema before touching it; if the columns are missing a new `.sqm` migration is required.
- `getTransactionsBySnapshot(snapshotId): Flow<List<SandboxTransaction>>` is already a reactive Flow in `SandboxRepositoryImpl` via SQLDelight's `.asFlow().mapToList()`. This is the foundation for the reactive STS computation.
- The home screen composable exposes an injection point for `SandboxViewModel` (or at minimum accepts a sandbox mode toggle callback). If the home screen is tightly coupled to a single ViewModel, a small refactor to accept an additional ViewModel may be needed.
- `CalculateSafeToSpendUseCase` (real STS) already exposes a `Flow<Long>` or can be collected reactively. This is required by `CompareSandboxWithRealityUseCase`. Verify before implementing the comparison flow.

### Risks

- **`GetSandboxSafeToSpendUseCase` signature change (`Result<Long>` → `Flow<Long>`)** will break any existing callers (`SandboxViewModel`, tests). Identify all call sites with `cocoindex-code` before changing the signature and update them in the same task.
- **`CompareSandboxWithRealityUseCase` signature change (`SandboxComparison` → `Flow<SandboxComparison>`)** has the same breakage risk. Same mitigation: find all callers first.
- **`SandboxHomeUiState` vs `SandboxUiState`** — Phase 6 built both. The home integration uses `SandboxHomeUiState`. Verify the ViewModel exposes `SandboxHomeUiState` (not just `SandboxUiState`) before building UI components that depend on it.
- **`getAllSnapshots` ordering** — Phase 6 built this query ordered by `created_at DESC`. The new spec requires `last_accessed_at DESC`. Changing this in the `.sq` file regenerates the SQLDelight query; verify no other screen depends on `created_at` ordering.
- **Coroutine leak on `setSandboxMode(false)`** — if `flatMapLatest` jobs are not properly scoped or cancelled, leaving sandbox mode may leave a live Flow collecting in the background. Tests must assert cancellation.

---

## Section 4 — Tasks

### Group A: SQLDelight Query Updates

- [ ] Use `cocoindex-code` to locate `sandbox.sq`. Verify `sandbox_snapshots` table has `description TEXT` and `initial_safe_to_spend INTEGER NOT NULL DEFAULT 0` columns. If either is missing, create a numbered `.sqm` migration file to add them. Update `insertSnapshot` query params to include `description` and `initial_safe_to_spend` if not already present. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** the `sandbox_snapshots` table exists **When** the schema is inspected **Then** both `description` and `initial_safe_to_spend` columns are present; if not, a migration adds them without data loss
  - **NFRs:** Migration must use a new `.sqm` number; never modify existing migration files

- [ ] Use `cocoindex-code` to locate `sandbox.sq`. Update `getAllSnapshots` query to order by `last_accessed_at DESC` (currently `created_at DESC`). Verify `updateLastAccessed` query exists (`UPDATE sandbox_snapshots SET last_accessed_at = :last_accessed_at WHERE id = :id`). If missing, add it. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** `getAllSnapshots` exists **When** the query is updated **Then** snapshots are returned ordered by most recently accessed first — the snapshot sheet bottom sheet will display them in this order
  - **NFRs:** See Section 1

- [ ] Use `cocoindex-code` to locate `sandbox_transactions.sq`. Verify `getSandboxBalanceDeltaBySnapshot` computes `Σ(income amounts) − Σ(expense amounts)` using a CASE expression. If the formula is incorrect (e.g., just summing all amounts), fix it to match: `COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0)`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** sandbox transactions with mixed income/expense types exist **When** `getSandboxBalanceDeltaBySnapshot` is executed **Then** the result equals the sum of income amounts minus sum of expense amounts; pure-expense sandbox returns a negative delta; pure-income sandbox returns a positive delta
  - **NFRs:** Correctness constraint — formula must match projected STS formula exactly

---

### Group B: Make Use Cases Reactive

- [ ] Use `cocoindex-code` to find `GetSandboxSafeToSpendUseCase` and all its call sites. Change its return type from `Result<Long>` to `Flow<Long>`. Implement by: calling `ISandboxRepository.getSnapshotById(snapshotId)` once to get `initialSafeToSpend`, then flat-mapping into `ISandboxRepository.getTransactionsBySnapshot(snapshotId)` (already a `Flow`) and mapping the transaction list to `initialSafeToSpend + Σ income - Σ expense`. Update all call sites. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** `getTransactionsBySnapshot` emits a new list after a DB insert **When** `GetSandboxSafeToSpendUseCase` collects that emission **Then** the projected STS value re-emits within 100ms without any external trigger
  - **NFRs:** All computation in `map {}` operator — no suspend calls inside the flow transform; must not block main thread

- [ ] Use `cocoindex-code` to find `CompareSandboxWithRealityUseCase` and all its call sites. Change its return type to `Flow<SandboxComparison>`. Implement by combining `GetSandboxSafeToSpendUseCase(snapshotId)` flow with the real STS flow (from `CalculateSafeToSpendUseCase` or `IBudgetRepository`). Use `combine(sandboxFlow, realStsFlow) { sandboxSts, realSts -> SandboxComparison(realSts, sandboxSts, sandboxSts - realSts) }`. Update all call sites. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** a real transaction is confirmed while sandbox is active **When** the real STS flow emits a new value **Then** `comparison.difference` updates automatically without user interaction
  - **NFRs:** `combine` must be used (not `zip`) so either upstream updating triggers a new emission; see Section 1

---

### Group C: Presentation Layer Updates

- [ ] Use `cocoindex-code` to find `SandboxHomeUiState`. Update the data class: remove `simulationInput: SimulationInput` and `consequencesResult: ConsequencesResult?`; add `sandboxTransactions: List<SandboxTransaction> = emptyList()`, `comparison: SandboxComparison? = null`, `isAddTransactionSheetVisible: Boolean = false`. Keep all other fields (`isSandboxMode`, `activeSnapshot`, `availableSnapshots`, `isSnapshotSheetVisible`, `projectedSafeToSpend`, `currentDailyRate`, `isLoading`, `error`). (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** the old ephemeral fields are removed **When** any file referencing `simulationInput` or `consequencesResult` on `SandboxHomeUiState` is compiled **Then** it fails to compile — fix all compilation errors in this task
  - **NFRs:** See Section 1

- [ ] Use `cocoindex-code` to find `SandboxViewModel`. Update `selectSnapshot(id: Long)` to: call `UpdateSnapshotLastAccessedUseCase`, then start collecting `GetSandboxTransactionsUseCase(id)`, `GetSandboxSafeToSpendUseCase(id)`, and `CompareSandboxWithRealityUseCase(id)` using `flatMapLatest` — all three scoped to a `selectedSnapshotJob` that is cancelled and replaced whenever `selectSnapshot` is called again. Emit results into `SandboxHomeUiState`. Add `showAddTransactionSheet()` and `hideAddTransactionSheet()` methods. Rename `addSimulation(req)` → `addTransaction(req: AddSandboxTransactionRequest)` and `removeSimulation(id)` → `removeTransaction(id: Long)`. Update `setSandboxMode(true)` to call `CheckAndExpireSandboxesUseCase` then `loadAvailableSnapshots()`. Update `setSandboxMode(false)` to cancel `selectedSnapshotJob`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** `selectSnapshot(id)` is called **When** a sandbox transaction is inserted via `addTransaction()` **Then** `SandboxHomeUiState.sandboxTransactions`, `projectedSafeToSpend`, and `comparison` all update without any additional method call
  - **Given** `setSandboxMode(false)` is called **When** the ViewModel is inspected **Then** `selectedSnapshotJob` is cancelled and no further Flow emissions update the state
  - **NFRs:** No coroutine leaks; `selectedSnapshotJob` must be cancelled before a new one is started on `selectSnapshot`

---

### Group D: UI Components

- [ ] Read node `mTtvR` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get`. Use `cocoindex-code` to find where the home screen renders `BcSegmentedControl`. Update the two labels to `"Live Budget"` (left, active when `!isSandboxMode`) and `"Sandbox"` (right, active when `isSandboxMode`). Wire `onToggle` to `sandboxViewModel.setSandboxMode(enabled)`. After coding, re-read `mTtvR` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** the home screen is displayed **When** the user taps "Sandbox" **Then** `SandboxViewModel.setSandboxMode(true)` is called, the segmented control shows "Sandbox" as active, and the content area below switches to sandbox content
  - **NFRs:** Labels must match the spec exactly — "Live Budget" not "Active", "Sandbox" not "Inactive"

- [ ] Build `SnapshotSelectorPill` composable in `features/sandbox/presentation/components/`: full-width `bgDark` pill (`radius.full`), showing `activeSnapshotName` left-aligned (`textInverted`, `bodyLarge`) and a flask/science icon right-aligned. When `activeSnapshot == null`, show "No Snapshot Selected". Tapping calls `onTap`. Only rendered when `isSandboxMode = true`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** sandbox mode is active and a snapshot is selected **When** the pill is rendered **Then** it shows the snapshot name and is tappable; when no snapshot is selected it shows the fallback text
  - **NFRs:** Touch target ≥ 44dp height; `bgDark` background token only — no raw hex

- [ ] Read node `qE6i6` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get`. Build `SnapshotSelectorSheet` composable as a `ModalBottomSheet`: `LazyColumn` of snapshot rows (name left, `lastAccessedAt` formatted right; active snapshot shows a checkmark icon); rows ordered by `lastAccessedAt DESC`; long-press on a row shows a `ConfirmDialog` (node `0JGwC`) before calling `onDelete(id)`; a "Create New" ghost button (`MO989`) at the bottom calls `onCreateNew`. Tapping a row calls `onSelect(id)` and dismisses. After coding, re-read `qE6i6` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** the snapshot sheet is visible **When** the user taps a snapshot row **Then** `onSelect(id)` is called and the sheet dismisses; when they long-press a row a confirm dialog appears before deletion
  - **NFRs:** See Section 1

- [ ] Read node `0yIOD` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get`. Use `cocoindex-code` to find `HeroSafeToSpend`. Build `HeroProjectedSpend` composable (or add a `label` parameter to the existing component) that renders with label `"PROJECTED SPEND"` (same style as `"SAFE TO SPEND"`). When `projectedSafeToSpend > 0`: display formatted amount in `textPrimary`. When `projectedSafeToSpend <= 0`: display formatted amount in `colorError` with a leading `AlertCircle` icon (16dp). After coding, re-read `0yIOD` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** sandbox expenses exceed `initialSafeToSpend` **When** `HeroProjectedSpend` renders **Then** the amount is red and the warning icon is visible alongside the text (not color alone)
  - **NFRs:** Accessibility — icon must accompany color signal; no raw hex or sp values

- [ ] Build `ComparisonDeltaRow` composable in `features/sandbox/presentation/components/`: a single horizontal row showing `"vs ₱X.XX real"` on the left and the signed delta (`"+₱X.XX"` or `"-₱X.XX"`) on the right. Delta is `colorSuccess` when positive, `colorError` when negative, `textSecondary` when zero. `bodyMedium` typography, `textSecondary` for the label. Only rendered when `comparison != null`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** `comparison.difference` is −₱2,000 **When** `ComparisonDeltaRow` renders **Then** the delta shows `"−₱2,000.00"` in `colorError`; when difference is +₱1,000 it shows green
  - **NFRs:** All color tokens from `BudgetCalendarTheme` — no raw hex

- [ ] Build `AddSandboxTransactionSheet` composable as a `ModalBottomSheet` in `features/sandbox/presentation/components/`: a `BcSegmentedControl` for type (Income / Expense); a `LargeAmountInput` (node `L2zpZ`) for amount; an `InputGroup` (node `c9XFF`) for category (label: "Name"); a `SelectGroup` (node `8G9pp`) for account (populated from real accounts); a date picker row defaulting to today; a full-width `BcButton(PRIMARY)` labeled "Add". Validates: category not blank, amount > 0 before enabling the Add button. Calls `onAdd(AddSandboxTransactionRequest)` on confirm, `onDismiss` on cancel/X. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** the sheet is open **When** the user leaves category blank or enters amount 0 **Then** the Add button is disabled; when both are valid the button is enabled and tapping it calls `onAdd`
  - **NFRs:** Touch targets ≥ 44dp; amount stored as cents (`Long`) — no `Double` conversion; account list comes from `AccountRepository`, not hardcoded

- [ ] Use `cocoindex-code` to find `SandboxTransactionItem`. Update it for the home screen context: show `type` badge (income = `bHBso` green badge, expense = `ywlyv` / red badge); add a trailing "Promote" ghost button (`MO989`) that shows a `ConfirmDialog` before calling `onPromote(id)`; add a swipe-to-delete or trailing "Remove" destructive button (`Ifqk0`) that calls `onRemove(id)`. Hide the Promote button when `originalTransactionId != null`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** a sandbox transaction with type INCOME is rendered **When** displayed **Then** a green badge is visible; tapping Promote shows a confirmation dialog; tapping Remove without confirmation calls `onRemove` (or shows confirm if destructive pattern is used)
  - **NFRs:** See Section 1

- [ ] Build `SandboxTransactionList` composable in `features/sandbox/presentation/components/`: a `Section Header` (node `O3ux1`) with title "Sandbox Transactions" and an "Add" ghost button (`MO989`) on the right that calls `onAddTap`; a `LazyColumn` of `SandboxTransactionItem` rows below. When `transactions.isEmpty()`: show an empty state — "No transactions yet. Tap Add to start planning." centered with the Add button below it. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** no transactions have been added to the sandbox **When** `SandboxTransactionList` renders **Then** the empty state message and Add button are visible; when transactions exist the `LazyColumn` renders them
  - **NFRs:** See Section 1

---

### Group E: Home Screen Wiring

- [ ] Use `cocoindex-code` to find the home screen composable. Inject `SandboxViewModel` via `koinViewModel()`. Read `SandboxHomeUiState` from it. Wire the screen so: when `isSandboxMode = false` → render existing home content (STS hero, week calendar, overdue list); when `isSandboxMode = true` → render `SandboxHomeContent` in order: `SnapshotSelectorPill`, `HeroProjectedSpend`, `ComparisonDeltaRow`, `SandboxTransactionList`. Show `SnapshotSelectorSheet` as a `ModalBottomSheet` when `isSnapshotSheetVisible = true`. Show `AddSandboxTransactionSheet` when `isAddTransactionSheetVisible = true`. Pass the header title as `if (isSandboxMode) "Sandbox" else "Budget"` to `BcHeader`. Add `DisposableEffect(Unit) { onDispose { sandboxViewModel.setSandboxMode(false) } }` to reset on screen leave. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** sandbox mode is active and the user navigates to Calendar tab **When** they return to Home **Then** `setSandboxMode(false)` was called on leave and `setSandboxMode(true)` must be called again to re-enter sandbox mode; the DB state (sandbox transactions) is still intact
  - **Given** `isSandboxMode = true` and `isSnapshotSheetVisible = true` **When** the screen renders **Then** the `SnapshotSelectorSheet` bottom sheet is visible over the sandbox content
  - **NFRs:** No layout jank on the content swap; transition should be instantaneous (no animation required)

---

### Group F: Tests

- [ ] Use `cocoindex-code` to find `FakeSandboxRepository`. Update it to support reactive Flow emissions: `getTransactionsBySnapshot` should use a `MutableStateFlow<List<SandboxTransaction>>` internally so that `insertSandboxTransaction` and `deleteSandboxTransaction` trigger new emissions. This makes ViewModel and use case tests reactive without a real DB. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** `FakeSandboxRepository.insertSandboxTransaction` is called **When** a collector on `getTransactionsBySnapshot` is active **Then** it receives a new emission with the updated list
  - **NFRs:** No real coroutine delay; use `MutableStateFlow` not `MutableSharedFlow` so collectors always receive the latest state

- [ ] Use `cocoindex-code` to find `SandboxUseCasesTest`. Add tests for updated `GetSandboxSafeToSpendUseCase`: (1) initial emission equals `initialSafeToSpend` when no transactions exist; (2) adding an EXPENSE transaction emits `initialSafeToSpend - amount`; (3) adding an INCOME transaction emits `initialSafeToSpend + amount`; (4) adding both emits the correct combined value; (5) flow emits a new value after `FakeSandboxRepository` insert (reactive assertion). (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** `GetSandboxSafeToSpendUseCase` collects on a `TestScope` **When** `FakeSandboxRepository` emits a new transaction list **Then** the use case emits a new projected STS value automatically
  - **NFRs:** Use `turbine` or `kotlinx-coroutines-test` `runTest` with `awaitItem()` for Flow assertions

- [ ] Add unit tests for `CompareSandboxWithRealityUseCase` reactive behavior: (1) when sandbox has no transactions, `difference == 0`; (2) when an EXPENSE is added to sandbox, `difference` is negative; (3) when real STS changes (real Flow emits new value), `SandboxComparison` re-emits with updated `realSafeToSpend` and recalculated `difference`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** both the sandbox STS flow and real STS flow are collected **When** the real STS flow emits a new value **Then** `CompareSandboxWithRealityUseCase` emits a new `SandboxComparison` without any additional trigger
  - **NFRs:** See Section 1

- [ ] Add `SandboxViewModel` unit tests using `FakeSandboxRepository` and `TestScope`: (1) `selectSnapshot(id)` starts collecting all three flows and updates `SandboxHomeUiState`; (2) `addTransaction(req)` inserts to repository and `sandboxTransactions` + `projectedSafeToSpend` update automatically; (3) `removeTransaction(id)` removes from repository and state updates; (4) `setSandboxMode(false)` cancels active flow collection and `isSandboxMode` becomes false; (5) `promoteTransaction(id)` removes from sandbox and does not affect `projectedSafeToSpend` computation going forward. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
  - **Given** `selectSnapshot(id)` is called and then `setSandboxMode(false)` is called **When** a sandbox transaction is subsequently inserted via `FakeSandboxRepository` directly **Then** `SandboxHomeUiState` does NOT update (flow was cancelled)
  - **NFRs:** Use `StandardTestDispatcher` to control coroutine execution; assert no active jobs after `setSandboxMode(false)`

---

## Section 5 — Definition of Done

- [ ] All tasks above are implemented and committed
- [ ] Acceptance criteria pass for every task
- [ ] `projectedSafeToSpend` updates on screen within 100ms of a sandbox transaction add/remove (verified manually on device or emulator)
- [ ] Sandbox data persists after app kill and restore — verified by adding transactions, force-stopping, reopening, and confirming state is intact
- [ ] Real STS is unaffected by any sandbox transaction — verified by adding sandbox expenses and confirming home screen STS (Live Budget mode) does not change
- [ ] All sandbox use case tests pass (`./gradlew :composeApp:testDebugUnitTest --tests "*.sandbox.*"`)
- [ ] No compilation errors or warnings introduced
- [ ] Shadow spec updated if implementation deviated from original design (`specs/shadow/Sandbox.shadow.md`)
