# Phase 6: Sandbox Mode (What-If)

> Isolated simulation environment for financial scenario planning with real-time comparison against live data

**Shadow Spec:** specs/shadow/Sandbox.shadow.md

---

## Group A: Database & SQLDelight

- [x] Create `sandbox.sq` SQLDelight file in `core/database/` with queries: `getAllSnapshots`, `getSnapshotById`, `insertSnapshot`, `updateLastAccessed`, `deleteSnapshot`, `getExpiredSnapshots` (snapshots where `last_accessed_at < :cutoff`) (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Create `sandbox_transactions.sq` SQLDelight file with queries: `getSandboxTransactionsBySnapshot`, `insertSandboxTransaction`, `deleteSandboxTransaction`, `deleteSandboxTransactionsBySnapshot`, `getSandboxBalanceDeltaBySnapshot` (sum of amounts grouped by type for a given snapshot) (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Create a numbered `.sqm` migration file that adds `sandbox_snapshots` table (columns: `id`, `name`, `description`, `created_at`, `last_accessed_at`, `initial_safe_to_spend`; indexes: `idx_snapshots_created`) and `sandbox_transactions` table (columns: `id`, `snapshot_id` FK → `sandbox_snapshots(id)` ON DELETE CASCADE, `account_id`, `amount`, `date`, `type`, `status`, `description`, `category`, `original_transaction_id`, `created_at`, `updated_at`; indexes: `idx_sandbox_snapshot`, `idx_sandbox_date`) (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

## Group B: Domain Layer

- [x] Define `SandboxSnapshot` (id, name, description, createdAt, lastAccessedAt, initialSafeToSpend), `SandboxTransaction` (id, snapshotId, accountId, amount, date, type, status, description, category, originalTransactionId, createdAt, updatedAt), and `SandboxComparison` (realSafeToSpend, sandboxSafeToSpend, difference, addedTransactions, promotedCount) data classes in `features/sandbox/domain/model/` — all monetary fields as `Long` (cents) (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Define `ISandboxRepository` interface in `features/sandbox/domain/repository/` with: `getAllSnapshots(): Flow<List<SandboxSnapshot>>`, `getSnapshotById(id: Long): Result<SandboxSnapshot>`, `createSnapshot(name, description, initialSafeToSpend): Result<SandboxSnapshot>`, `updateLastAccessed(id: Long): Result<Unit>`, `deleteSnapshot(id: Long): Result<Unit>`, `getTransactionsBySnapshot(snapshotId: Long): Flow<List<SandboxTransaction>>`, `insertSandboxTransaction(snapshotId, accountId, amount, date, type, status, description, category, originalTransactionId): Result<SandboxTransaction>`, `deleteSandboxTransaction(id: Long): Result<Unit>`, `getSandboxBalanceDelta(snapshotId: Long): Result<Long>`, `deleteExpiredSnapshots(cutoffMs: Long): Result<Int>` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `CreateSandboxUseCase`: validates name (1–50 chars, trimmed, non-blank); calls `CalculateSafeToSpendUseCase` for current real value; persists via `ISandboxRepository.createSnapshot`; returns `Result<SandboxSnapshot>` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `GetSandboxesUseCase`: returns `Flow<List<SandboxSnapshot>>` ordered by `createdAt` desc from repository (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `AddSimulationTransactionUseCase`: validates amount > 0 and account exists; sets status to PENDING; inserts via `ISandboxRepository.insertSandboxTransaction`; updates `lastAccessedAt`; returns `Result<SandboxTransaction>` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `RemoveSimulationTransactionUseCase`: deletes sandbox transaction by id via repository; updates `lastAccessedAt`; returns `Result<Unit>` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `GetSandboxSafeToSpendUseCase`: reads `initialSafeToSpend` from snapshot; subtracts sum of EXPENSE sandbox transactions; adds sum of INCOME sandbox transactions; clamps to 0 minimum; returns `Result<Long>` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `CompareSandboxWithRealityUseCase`: calls `CalculateSafeToSpendUseCase` for real value and `GetSandboxSafeToSpendUseCase` for sandbox value; computes `difference = sandboxSafeToSpend - realSafeToSpend`; returns `SandboxComparison` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `PromoteTransactionUseCase`: maps `SandboxTransaction` fields to a real `CreateTransactionRequest`; calls `ITransactionRepository.insert`; calls `ISandboxRepository.deleteSandboxTransaction`; returns `Result<Transaction>` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `DeleteSandboxUseCase`: calls `ISandboxRepository.deleteSnapshot` (cascade removes sandbox_transactions via DB FK); returns `Result<Unit>` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `CheckAndExpireSandboxesUseCase`: computes cutoff = `now - 30 days` in milliseconds; calls `ISandboxRepository.deleteExpiredSnapshots(cutoff)`; returns `Result<Int>` (count of deleted sandboxes) (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

## Group C: Data Layer

- [x] Implement `SandboxMapper` in `features/sandbox/data/mapper/`: converts SQLDelight `SandboxSnapshotsEntity` → `SandboxSnapshot` and `SandboxTransactionsEntity` → `SandboxTransaction` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `SandboxRepositoryImpl` implementing `ISandboxRepository` in `features/sandbox/data/repository/`: delegates all operations to SQLDelight queries from `sandbox.sq` / `sandbox_transactions.sq`; wraps results with `SandboxMapper`; emits `Flow` via `.asFlow()` on SQLDelight queries (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

## Group D: Dependency Injection

- [x] Register `ISandboxRepository` → `SandboxRepositoryImpl` (singleton) in Koin `repositoryModule` in `di/KoinModules.kt` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Register all sandbox use cases (`CreateSandboxUseCase`, `GetSandboxesUseCase`, `AddSimulationTransactionUseCase`, `RemoveSimulationTransactionUseCase`, `GetSandboxSafeToSpendUseCase`, `CompareSandboxWithRealityUseCase`, `PromoteTransactionUseCase`, `DeleteSandboxUseCase`, `CheckAndExpireSandboxesUseCase`) as `factory { }` in Koin `useCaseModule` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

## Group E: Presentation Layer

- [x] Define `SandboxUiState` data class in `features/sandbox/presentation/`: `snapshots: List<SandboxSnapshot>`, `activeSnapshot: SandboxSnapshot?`, `sandboxTransactions: List<SandboxTransaction>`, `sandboxSafeToSpend: Long`, `comparison: SandboxComparison?`, `isLoading: Boolean`, `isComparing: Boolean`, `error: String?` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `SandboxViewModel` in `features/sandbox/presentation/`: exposes `SandboxUiState` via `StateFlow`; collects `GetSandboxesUseCase` on init; provides `createSandbox(name, desc)`, `loadSandbox(id)`, `addSimulation(req)`, `removeSimulation(id)`, `toggleComparison()`, `promoteToReal(transaction)`, `deleteSandbox(id)`, `exitSandbox()` methods; calls `CheckAndExpireSandboxesUseCase` on init (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Register `SandboxViewModel` in Koin `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

## Group F: UI Components

- [x] Implement `SandboxListScreen` composable: `LazyColumn` of sandbox cards (name, created date, initial Safe to Spend snapshot); FAB to create new sandbox; empty-state ("No sandboxes — create one to simulate scenarios"); each card taps to `SandboxDetailScreen`; long-press shows delete confirmation dialog; collects `SandboxUiState` from `SandboxViewModel` via `koinViewModel()` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `CreateSandboxDialog` composable: text fields for name (required, max 50) and description (optional, max 200); confirm/cancel buttons; shows inline validation errors; calls `onConfirm(name, description)` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `SandboxDetailScreen` composable: purple-tinted top header (`#F3E5F5` background) showing sandbox name + sandbox Safe to Spend amount; "Compare" toggle button; `LazyColumn` of `SandboxTransactionItem`; FAB to add simulation transaction (reuses transaction form sheet with sandbox flag); shows `ComparisonDisplay` when `isComparing == true` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `ComparisonDisplay` composable: two-column layout — "Real" (teal) vs "Sandbox" (purple) Safe to Spend amounts; difference row below with arrow icon (green if sandbox > real, red if less); collapses/expands on tap (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Implement `SandboxTransactionItem` composable: transaction row with amount, category, date; trailing icon row with "Promote" (arrow-up icon) and "Remove" (delete icon) buttons; "Promote" button hidden if `originalTransactionId != null` (already real); shows confirmation bottom sheet before promote (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Add **Sandbox** as a fifth destination to the bottom `NavigationBar` in `App.kt` (use `science` or `bubble_chart` Material icon); wire `SandboxListScreen` and add back-stack navigation to `SandboxDetailScreen` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)

---

## Group G: Tests

- [x] Create `FakeSandboxRepository` implementing `ISandboxRepository` with configurable in-memory `MutableList<SandboxSnapshot>` and `MutableList<SandboxTransaction>` in `commonTest/kotlin/.../features/sandbox/` (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Test `CreateSandboxUseCase`: valid name creates snapshot with correct `initialSafeToSpend`; name empty → validation error; name > 50 chars → validation error (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Test `AddSimulationTransactionUseCase`: valid EXPENSE inserts transaction; amount ≤ 0 → validation error; snapshot not found → error (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Test `GetSandboxSafeToSpendUseCase`: no transactions → equals `initialSafeToSpend`; adding EXPENSE → decreases correctly; adding INCOME → increases correctly; result never negative (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Test `CompareSandboxWithRealityUseCase`: difference = sandbox − real computed correctly; sandbox with no transactions → difference is 0; negative difference shown correctly (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Test `PromoteTransactionUseCase`: sandbox transaction removed after promotion; real transaction created with matching fields; promoting already-real transaction (originalTransactionId set) → still removes from sandbox (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
- [x] Test `CheckAndExpireSandboxesUseCase`: sandbox last accessed 31 days ago → deleted; sandbox last accessed 29 days ago → preserved; returns correct count of deleted sandboxes (Use Skill: kmp) (Reference: specs/shadow/Sandbox.shadow.md)
