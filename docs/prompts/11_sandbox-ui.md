# Sandbox UI — Home Screen Integration

**Feature:** Sandbox Mode (What-If) — Home Screen UI Layer
**Shadow Spec:** `specs/shadow/Sandbox.shadow.md`
**Design Rules:** SAN-011 through SAN-015 (`specs/FEATURES.md`)

---

## Context

The Sandbox view is an in-screen content swap on the Home tab driven by the segmented control. Selecting "Sandbox" replaces the Safe to Spend / week calendar / overdue content with: a snapshot selector pill, a Projected Spend hero, an ephemeral Simulate Expense form, and a Consequences section that appears after "Run Simulation" is tapped. Nothing in the simulation is persisted — it all resets on leave.

Implementation order: DB schema → domain models → repository → use cases → ViewModel → DI → UI components → home screen wiring → tests.

---

## Tasks

### 1. DB Schema — add `initial_safe_to_spend` to sandbox_snapshots

- [ ] Use `cocoindex-code` to locate the SQLDelight `.sq` file for `sandbox_snapshots`. Verify the current schema matches `specs/DB_DESIGN.md`. Add the `initial_safe_to_spend INTEGER NOT NULL DEFAULT 0` column if missing. Add `getAllSnapshots` query ordered by `last_accessed_at DESC`. Add `updateSnapshotLastAccessed` query. If the table already has data, create a numbered `.sqm` migration file. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 2. Domain Models

- [ ] Use `cocoindex-code` to search for any existing `SandboxSnapshot` or `SandboxTransaction` domain model files. Create or update `features/sandbox/domain/model/SandboxSnapshot.kt` to match the spec interface (id, name, description, createdAt, lastAccessedAt, initialSafeToSpend as Long). Create `features/sandbox/domain/model/SimulationInput.kt` (purchaseName: String, amount: Long). Create `features/sandbox/domain/model/ConsequencesResult.kt` (newSafeToSpend: Long, dailyVelocityImpact: Long, daysOfRunway: Int, isAffordable: Boolean). All amounts in cents. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 3. Repository Interface

- [ ] Use `cocoindex-code` to find any existing `ISandboxRepository`. Create or update `features/sandbox/domain/repository/ISandboxRepository.kt` with: `getAllSnapshots(): Flow<List<SandboxSnapshot>>`, `getSnapshotById(id: Long): SandboxSnapshot?`, `createSnapshot(name: String, description: String?, initialSafeToSpend: Long): SandboxSnapshot`, `updateLastAccessed(id: Long)`, `deleteSnapshot(id: Long)`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 4. Repository Implementation

- [ ] Use `cocoindex-code` to find any existing `SandboxRepositoryImpl`. Create or update `features/sandbox/data/repository/SandboxRepositoryImpl.kt` implementing `ISandboxRepository`. Use the SQLDelight `sandbox_snapshots` queries. Map DB entity to `SandboxSnapshot` domain model via a mapper in `features/sandbox/data/mapper/SandboxMapper.kt`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 5. Use Cases

- [ ] Use `cocoindex-code` to search for existing sandbox use cases. Create the following use cases in `features/sandbox/domain/usecase/`:
  - `GetSandboxesUseCase` — returns `Flow<List<SandboxSnapshot>>` from repository
  - `GetSandboxByIdUseCase` — returns `SandboxSnapshot?` for a given id
  - `CreateSandboxUseCase` — takes name, description, currentSafeToSpend (Long); calls `repository.createSnapshot()`; returns `SandboxSnapshot`
  - `DeleteSandboxUseCase` — calls `repository.deleteSnapshot(id)`
  - `RunSimulationUseCase` — **pure computation, no DB access**: takes `SimulationInput`, `activeSnapshot: SandboxSnapshot`, `currentDailyRate: Long`, `daysRemainingInMonth: Int`; computes and returns `ConsequencesResult` per spec formulas (newSafeToSpend, dailyVelocityImpact, daysOfRunway = max(0, newSTS / rate), isAffordable = newSTS >= 0)
  (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 6. SandboxHomeUiState + SandboxViewModel

- [ ] Use `cocoindex-code` to find the existing `SandboxViewModel` if any. Create `features/sandbox/presentation/SandboxHomeUiState.kt` as a data class with all fields from the spec: `isSandboxMode`, `activeSnapshot`, `availableSnapshots`, `isSnapshotSheetVisible`, `projectedSafeToSpend`, `currentDailyRate`, `simulationInput`, `consequencesResult`, `isLoading`, `error`. Create or update `features/sandbox/presentation/SandboxViewModel.kt` with:
  - `setSandboxMode(enabled: Boolean)` — loads snapshots when true, clears simulation when false
  - `loadAvailableSnapshots()` — calls `GetSandboxesUseCase`, auto-selects most recently accessed
  - `selectSnapshot(id: Long)` — calls `GetSandboxByIdUseCase`, updates `activeSnapshot` + `projectedSafeToSpend`, calls `updateLastAccessed`
  - `showSnapshotSheet()` / `hideSnapshotSheet()`
  - `updateSimulationInput(input: SimulationInput)` — updates ephemeral form state
  - `runSimulation()` — validates input (name not blank, amount > 0), calls `RunSimulationUseCase`, updates `consequencesResult`
  - `clearSimulation()` — resets `simulationInput` to empty, sets `consequencesResult = null`
  - `createSandbox(name: String)` — calls `CreateSandboxUseCase` with current STS, reloads snapshots, auto-selects new sandbox
  - `deleteSandbox(id: Long)` — calls `DeleteSandboxUseCase`, reloads snapshots
  (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 7. DI Wiring

- [ ] Use `cocoindex-code` to locate `KoinModules.kt`. Add `ISandboxRepository` → `SandboxRepositoryImpl` to `repositoryModule`. Add all 5 sandbox use cases to `useCaseModule` as `factory { }`. Add or update `SandboxViewModel` in `viewModelModule` as `viewModel { }`. Verify no duplicate bindings exist. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 8. Segmented Control Labels Fix

- [ ] Read node `mTtvR` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get` to confirm the visual spec of the segmented control. Use `cocoindex-code` to find where the home screen renders `BcSegmentedControl` with "Active"/"Inactive". Change the labels to "Live Budget" (left, isActiveSelected = true when not sandbox) and "Sandbox" (right). Wire the `onToggle` callback to `sandboxViewModel.setSandboxMode()`. After coding, re-read node `mTtvR` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 9. Snapshot Selector Pill

- [ ] Use `cocoindex-code` to understand how the home screen header area is structured. Build `SnapshotSelectorPill` composable: a full-width dark pill (`bgDark` background, `radius.full`) showing the active snapshot name on the left and a flask icon (`Icons.Outlined.Science` or equivalent) on the right. When `activeSnapshot == null` show "No Snapshot Selected". Tapping calls `onSnapshotPillTap`. Only visible when `isSandboxMode = true`. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 10. Snapshot Selector Bottom Sheet

- [ ] Read node `qE6i6` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get` to confirm Results List styling. Build `SnapshotSelectorSheet` as a `ModalBottomSheet`: list of `SandboxSnapshot` items (name + created date, active one highlighted with a checkmark), ordered by `lastAccessedAt DESC`. At the bottom, a "Create New" ghost button. Tapping a snapshot calls `onSelect(id)` and dismisses. Tapping "Create New" shows a simple name-input dialog then calls `onCreateNew(name)`. After coding, re-read `qE6i6` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 11. Projected Spend Hero

- [ ] Read node `0yIOD` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get` to confirm Hero / Safe to Spend spec. Use `cocoindex-code` to find the existing `HeroSafeToSpend` composable. Build `HeroProjectedSpend` (or accept a `label` parameter in the existing component) that renders with label `"PROJECTED SPEND"` (uppercase, same style as SAFE TO SPEND) and displays `projectedSafeToSpend` formatted via `CurrencyUtils`. The daily rate sub-label shows `currentDailyRate / day`. After coding, re-read `0yIOD` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 12. Simulation Form Card

- [ ] Read node `Ksdjt` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get` to confirm Simulation Form spec. Build `SimulationFormCard` composable: `bgSurface` background card (`radius.xl`), two inputs side by side (Purchase Name text field, Amount number field with currency prefix), and a full-width "Run Simulation" `BcButton(variant = PRIMARY)` below. All input changes call `onSimulationInputChange`. The Section Header above the card shows "Simulate Expense" title and a "Clear" ghost button that calls `onClearSimulation`. After coding, re-read `Ksdjt` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 13. Consequences Section

- [ ] Read node `qE6i6` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get` to confirm Results List spec. Build `ConsequencesSection` composable wrapped in `AnimatedVisibility(visible = consequencesResult != null)`. Inside, render four `ConsequenceRow` items using the Results List container:
  1. **New Safe to Spend** — formatted currency value; `colorError` if < 0, `textPrimary` otherwise; subtitle "After simulated expense"
  2. **Daily Velocity Impact** — signed formatted value with `/day`; `colorError` if negative; subtitle "Safe to spend change per day"
  3. **Days of Runway** — integer days; `colorError` if 0; subtitle "Until Safe to Spend reaches zero"
  4. **Affordability** — "Affordable" in `colorSuccess` or "Cannot afford" in `colorError`; no subtitle
  After coding, re-read `qE6i6` and confirm visual fidelity. (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 14. Home Screen Wiring

- [ ] Use `cocoindex-code` to find the home screen composable (likely `HomeScreen.kt` or within `App.kt`). Inject `SandboxViewModel` via Koin. Wire the screen so:
  - When `isSandboxMode = false`: render existing home content (Safe to Spend hero, week calendar, overdue list)
  - When `isSandboxMode = true`: render `SandboxHomeContent` in order — `SnapshotSelectorPill`, `HeroProjectedSpend`, `SimulationFormCard`, `ConsequencesSection`
  - Header title: pass `if (isSandboxMode) "Sandbox" else "Budget"` to `BcHeader`
  - `SnapshotSelectorSheet` shown when `isSnapshotSheetVisible = true`
  - On `DisposableEffect(Unit) { onDispose { sandboxViewModel.setSandboxMode(false) } }` to reset on leave
  (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

### 15. Unit Tests — RunSimulationUseCase

- [ ] Use `cocoindex-code` to find existing test patterns in `composeApp/src/commonTest/`. Write unit tests for `RunSimulationUseCase` covering:
  - Affordable scenario: `newSafeToSpend >= 0`, verify `isAffordable = true`, `daysOfRunway > 0`, correct `dailyVelocityImpact` sign
  - Unaffordable scenario: `simulationInput.amount > snapshot.initialSafeToSpend`, verify `newSafeToSpend < 0`, `isAffordable = false`, `daysOfRunway = 0`
  - Zero daily rate edge case: `currentDailyRate = 0` should not throw (guard division)
  - Exact boundary: `amount == initialSafeToSpend` → `newSafeToSpend = 0`, `isAffordable = true`
  (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
