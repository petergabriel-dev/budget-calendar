# Phase 5: Budget / Safe to Spend

> Real-time Safe to Spend calculation with spending pool aggregation, credit card reservations, and monthly rollover

**Shadow Spec:** specs/shadow/Budget.shadow.md

---

## Group A: Database & SQLDelight

- [x] Create `budget.sq` SQLDelight file in `core/database/` with queries: `getTotalSpendingPoolBalance`, `getPendingAndOverdueForSpendingPool`, `getCreditCardReservedAmount`, `getCreditCardReservations` (aggregate per CC account) (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Create `monthly_rollovers.sq` SQLDelight file with queries: `getRolloverByMonth`, `getAllRollovers`, `insertRollover`, `upsertRollover` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Create `credit_card_settings.sq` SQLDelight file with queries: `getCreditCardSettingsByAccount`, `upsertCreditCardSettings`, `deleteCreditCardSettings` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Create a numbered `.sqm` migration file that adds `monthly_rollovers` table (with `UNIQUE(year, month)` and `idx_rollovers_year_month` index) and `credit_card_settings` table (with `idx_cc_settings_account` index) (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

---

## Group B: Domain Layer

- [x] Define `SpendingPool`, `BudgetSummary`, `MonthlyRollover`, and `CreditCardReservation` data classes in `features/budget/domain/model/` — all monetary fields as `Long` (cents) (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Define `IBudgetRepository` interface with: `getTotalSpendingPoolBalance(): Flow<Long>`, `getPendingReservations(): Flow<Long>`, `getOverdueReservations(): Flow<Long>`, `getCreditCardReservations(): Flow<List<CreditCardReservation>>` in `features/budget/domain/repository/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Define `IMonthlyRolloverRepository` interface with: `getRolloverForMonth(year, month): Result<MonthlyRollover?>`, `getAllRollovers(): Flow<List<MonthlyRollover>>`, `saveRollover(year, month, amount): Result<MonthlyRollover>` in `features/budget/domain/repository/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `CalculateSafeToSpendUseCase`: `availableToSpend = totalBalance - pendingReservations - overdueReservations`; returns `Flow<BudgetSummary>` combining spending pool balance, pending/overdue sums, CC reserved, and latest rollover (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `GetCreditCardReservationsUseCase`: returns `Flow<List<CreditCardReservation>>` grouping pending/overdue EXPENSE transactions by CC account (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `SaveMonthlyRolloverUseCase`: validates year (≥ 2000) and month (1–12); upserts via `IMonthlyRolloverRepository`; returns `Result<MonthlyRollover>` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `GetRolloverHistoryUseCase`: returns `Flow<List<MonthlyRollover>>` ordered by year desc, month desc from repository (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `CalculateMonthEndRolloverUseCase`: reads spending pool balance at a given month end, subtracts confirmed expenses for that month, returns `Long` rollover amount; clamps negative result to 0 (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

---

## Group C: Data Layer

- [x] Implement `BudgetMapper` — converts raw SQLDelight `Long` query results and CC reservation rows to domain models in `features/budget/data/mapper/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `BudgetRepositoryImpl` implementing `IBudgetRepository` using SQLDelight queries from `budget.sq`; wire mapper; emit via `Flow` using `asFlow()` on SQLDelight queries in `features/budget/data/repository/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `MonthlyRolloverRepositoryImpl` implementing `IMonthlyRolloverRepository` using `monthly_rollovers.sq` queries in `features/budget/data/repository/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

---

## Group D: Dependency Injection

- [x] Register `IBudgetRepository` → `BudgetRepositoryImpl` (singleton) and `IMonthlyRolloverRepository` → `MonthlyRolloverRepositoryImpl` (singleton) in Koin `repositoryModule` in `di/KoinModules.kt` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Register all budget use cases (`CalculateSafeToSpendUseCase`, `GetCreditCardReservationsUseCase`, `SaveMonthlyRolloverUseCase`, `GetRolloverHistoryUseCase`, `CalculateMonthEndRolloverUseCase`) as `factory { }` in Koin `useCaseModule` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

---

## Group E: Presentation Layer

- [x] Define `BudgetUiState` data class: `budgetSummary: BudgetSummary`, `spendingPoolAccounts: List<Account>`, `creditCardReservations: List<CreditCardReservation>`, `isLoading: Boolean`, `isCalculating: Boolean`, `error: String?` in `features/budget/presentation/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `BudgetViewModel`: collects `CalculateSafeToSpendUseCase` and `GetCreditCardReservationsUseCase` flows; exposes `BudgetUiState` via `StateFlow`; provides `refresh()`, `saveRollover(year, month)`, `loadRolloverHistory()` methods (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Register `BudgetViewModel` in Koin `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

---

## Group F: UI Components

- [x] Implement `SafeToSpendHeader` composable: centered large amount display (font-display-large 34sp bold), "Safe to Spend" label below, last-updated timestamp bottom-right, teal accent background (`#26A69A` @ 20% opacity), `onRefresh` pull gesture; show shimmer while `isCalculating == true` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `BudgetSummaryCard` composable: expandable card showing rows for Spending Pool Total, Pending Reservations (red), Overdue Reservations (red), Confirmed This Month, Rollover From Last Month; each row: label + formatted `CurrencyUtils` amount; `onDetailsTap` callback (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `CreditCardReservationItem` composable: CC account name + reserved amount chip + `onPaymentTap` button ("Pay Now"); if `reservedAmount == 0` show "$0.00 reserved" in muted grey (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `BudgetScreen` composable: `SafeToSpendHeader` at top, `BudgetSummaryCard`, `LazyColumn` of `CreditCardReservationItem`s (only for CC accounts), empty-state if no spending pool accounts; collects `BudgetUiState` from `BudgetViewModel` via `koinViewModel()`; shows `SnackBar` on error (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Integrate `SafeToSpendHeader` into `CalendarScreen` as a sticky top banner — the header should reflect `BudgetViewModel` state and update reactively when transactions change (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Add **Budget** as a fourth destination to the bottom `NavigationBar` in `App.kt`; wire `BudgetScreen` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

---

## Group G: Tests

- [x] Create `FakeBudgetRepository` implementing `IBudgetRepository` with configurable in-memory `Long` values and `FakeMonthlyRolloverRepository` implementing `IMonthlyRolloverRepository` in `commonTest/kotlin/.../features/budget/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `CalculateSafeToSpendUseCase`: correct Safe to Spend = pool balance − pending − overdue; no accounts in pool → 0; all pending cancelled → full balance; negative result → 0 (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `GetCreditCardReservationsUseCase`: CC with pending expenses → correct reserved amount; CC with no pending → 0; multiple CC accounts → separate reservations returned (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `SaveMonthlyRolloverUseCase`: valid year/month persisted; month < 1 or > 12 → validation error; year < 2000 → validation error; saving same year/month twice → upsert (no duplicate) (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `CalculateMonthEndRolloverUseCase`: rollover = pool balance − confirmed expenses; negative result clamped to 0; month with no confirmed expenses → full balance as rollover (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
