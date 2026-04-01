# Phase 4: Recurring Transactions

> Scheduled income/expense/transfer automation — monthly generation, active/inactive toggle, and UI management

**Shadow Spec:** specs/shadow/RecurringTransaction.shadow.md

---

## Group A: Database & SQLDelight

- [x] Create `recurring_transactions.sq` SQLDelight file in `core/database/` with the `recurring_transactions` table schema (id, account_id, destination_account_id, amount, day_of_month, type, description, is_active, created_at, updated_at) including CHECK constraints and indexes (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Add all SQLDelight queries to `recurring_transactions.sq`: `getAllRecurringTransactions`, `getActiveRecurringTransactions`, `getRecurringTransactionById`, `getRecurringTransactionsByAccount`, `insertRecurringTransaction`, `updateRecurringTransaction`, `toggleRecurringActive`, `deleteRecurringTransaction` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Create a numbered `.sqm` SQLDelight migration file that adds the `recurring_transactions` table and its three indexes (`idx_recurring_active`, `idx_recurring_day`, `idx_recurring_account`) (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)

---

## Group B: Domain Layer

- [x] Define `RecurringTransaction` data class and `RecurrenceType` enum (`INCOME`, `EXPENSE`, `TRANSFER`) in `features/recurring/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Define `CreateRecurringTransactionRequest`, `UpdateRecurringTransactionRequest`, and `GeneratedTransaction` data classes in `features/recurring/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Define `IRecurringTransactionRepository` interface with all function signatures (`getAll`, `getActive`, `getById`, `getByAccount`, `insert`, `update`, `toggleActive`, `delete`) in `features/recurring/domain/repository/` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `CreateRecurringTransactionUseCase`: validate amount > 0, dayOfMonth 1–31, TRANSFER requires `destinationAccountId`, source ≠ destination; insert via `IRecurringTransactionRepository` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `GetRecurringTransactionsUseCase` and `GetActiveRecurringTransactionsUseCase` — return `Flow<List<RecurringTransaction>>` from repository (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `UpdateRecurringTransactionUseCase`: validate fields (same rules as create); persist via repository (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `ToggleRecurringActiveUseCase`: calls `toggleRecurringActive` on repository; on activation, delegates to `GenerateMonthlyTransactionsUseCase` for future months only (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `DeleteRecurringTransactionUseCase`: deletes the recurring record only; already-generated `Transaction` rows are left untouched (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `GenerateMonthlyTransactionsUseCase`: for each active recurring, check if a PENDING transaction already exists for the current (year, month, recurringId) pair; if not, create via `ITransactionRepository`; clamp day 29–31 to last day of month (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `GetUpcomingGeneratedTransactionsUseCase`: calculate occurrences for the next N months per active recurring; cross-reference existing transactions; set `willGenerate = false` for already-created months; return `Flow<List<GeneratedTransaction>>` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)

---

## Group C: Data Layer

- [x] Implement `RecurringTransactionMapper` — maps SQLDelight generated entity to `RecurringTransaction` domain model in `features/recurring/data/mapper/` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `RecurringTransactionRepositoryImpl` implementing `IRecurringTransactionRepository` using SQLDelight queries; wire mapper for all results in `features/recurring/data/repository/` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)

---

## Group D: Dependency Injection

- [x] Register `IRecurringTransactionRepository` → `RecurringTransactionRepositoryImpl` (singleton) in Koin `repositoryModule` in `di/KoinModules.kt` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Register all recurring use cases (`CreateRecurringTransactionUseCase`, `GetRecurringTransactionsUseCase`, `GetActiveRecurringTransactionsUseCase`, `UpdateRecurringTransactionUseCase`, `ToggleRecurringActiveUseCase`, `DeleteRecurringTransactionUseCase`, `GenerateMonthlyTransactionsUseCase`, `GetUpcomingGeneratedTransactionsUseCase`) as `factory { }` in Koin `useCaseModule` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)

---

## Group E: Presentation Layer

- [x] Define `RecurringUiState` data class: `recurringTransactions: List<RecurringTransaction>`, `upcomingGenerated: List<GeneratedTransaction>`, `isLoading: Boolean`, `error: String?`, `showForm: Boolean`, `editingRecurring: RecurringTransaction?` in `features/recurring/presentation/` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `RecurringViewModel` with `loadRecurring()`, `createRecurring(CreateRecurringTransactionRequest)`, `updateRecurring(id, request)`, `toggleActive(id, isActive)`, `deleteRecurring(id)`, `generateMonthly()` — wires all recurring use cases; emits `RecurringUiState` via `StateFlow` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Register `RecurringViewModel` in Koin `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)

---

## Group F: UI Components

- [x] Implement `RecurringCard` composable: type icon (income = green down-arrow, expense = red up-arrow, transfer = swap-arrows), formatted amount, "Day X of month" label, next scheduled date, active/inactive status dot (green #4CAF50 / grey #9E9E9E); calls `onTap` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `RecurringListScreen` composable: `LazyColumn` of `RecurringCard`s, `FloatingActionButton` triggering `onAddRecurring`, empty-state prompt ("No recurring transactions"), monthly total summary row (income / expense / net) (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `RecurringFormSheet` composable (modal bottom sheet): type tab row (INCOME / EXPENSE / TRANSFER), amount `TextField` (numeric), day-of-month selector (1–31), account `DropdownMenu`, destination account `DropdownMenu` visible only for TRANSFER, description `TextField` (optional, max 200 chars); inline validation error text; Save / Cancel buttons; Delete button visible only when editing (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Implement `RecurringScreen` composable: hosts `RecurringListScreen` + `RecurringFormSheet`; collects `RecurringUiState` from `RecurringViewModel` via `koinViewModel()`; shows `SnackBar` on error (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Add **Recurring** as a third destination to the bottom `NavigationBar` in `App.kt` (alongside Calendar and Accounts); wire `RecurringScreen` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)

---

## Group G: Tests

- [x] Create `FakeRecurringTransactionRepository` implementing `IRecurringTransactionRepository` with in-memory `MutableList<RecurringTransaction>` in `commonTest/kotlin/.../features/recurring/` (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Test `CreateRecurringTransactionUseCase`: valid INCOME created with correct fields; amount ≤ 0 → error "Amount must be greater than zero"; dayOfMonth < 1 or > 31 → error; TRANSFER without destinationAccountId → error; same-account TRANSFER → error "Cannot transfer to the same account" (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Test `GenerateMonthlyTransactionsUseCase`: active recurring generates PENDING transaction for current month; second call same month → skipped (no duplicate); inactive recurring → not generated; dayOfMonth 31 in February → clamped to last day of month (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Test `ToggleRecurringActiveUseCase`: deactivating keeps existing pending transactions untouched; activating triggers generation for current/future months only (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
- [x] Test `DeleteRecurringTransactionUseCase`: recurring record deleted; previously generated transactions remain in repository unchanged (Use Skill: kmp) (Reference: specs/shadow/RecurringTransaction.shadow.md)
