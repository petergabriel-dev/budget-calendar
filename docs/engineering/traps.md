# Traps

Current-code guards for bugs this project has already hit. `docs/engineering/` and code are authoritative.

## Mutable balance drift

- Symptom: confirmed transactions existed but account balance stayed stale (`bug-reports/account-management/2026-04-02_1200_stored-balance-permanently-drifts-from-transaction-history.md:11-30`).
- Root cause: old mutable `accounts.balance` depended on every write path calling `adjustBalance()`.
- Resolution: migration 7 renamed balance to `initial_balance` and added/backfilled `signed_amount`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/7.sqm:1-23`; fix plan: `docs/prompts/25_bugfix_computed-account-balance-from-transaction-history.md:1-19`.
- Current guard: balance reads compute `initial_balance + SUM(signed_amount)` for confirmed rows; sandbox transactions live in `sandbox_transactions`, not main `transactions`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:60-75`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:71-86`.

## SQLDelight flows do not refresh dependent repos by magic

- Symptom: STS/account UI did not update after transaction writes (`bug-reports/account-management/2026-04-01_1615_flow-observation-not-triggering.md:9-29`).
- Root cause: dependent SQLDelight `.asFlow()` queries emitted once or replayed stale values; `combine(trigger)` did not force re-query (`docs/prompts/24_bugfix_confirmed-transaction-not-updating-balance-and-sts.md:9-21`).
- Resolution: `transactionChangedTrigger` + `flatMapLatest` around SQLDelight query chains.
- Current guard: transaction writes emit trigger: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:127-206`; account/budget repos re-query via `flatMapLatest`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:31-62`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/data/repository/BudgetRepositoryImpl.kt:27-84`.

## Confirmed spending double-deduction

- Symptom: after confirming a pending expense, STS changed incorrectly (`bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md:9-18`).
- Root cause: confirmed expenses are already reflected in balance; subtracting them again double-counts them (`bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md:70-89`).
- Resolution: keep STS formula as pool balance minus pending/overdue/CC reserved; do not subtract `confirmedSpending` again.
- Current guard: `availableToSpend` excludes `confirmedSpending`, while still reporting it in summary: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:40-63`.

## STS confirmedSpending tests must use current-month dates

- Symptom: `calculateSafeToSpend_confirmedExpenseDoesNotIncreaseSTS` expected `confirmedSpending` but got 0 after calendar month changed.
- Root cause: `CalculateSafeToSpendUseCase` samples wall-clock `DateUtils.currentMonthBounds()` and reports confirmed spending only for EXPENSE rows inside that current month.
- Resolution: date STS confirmed-spending test transactions from `DateUtils.currentMonthBounds().first`, not hardcoded absolute dates that silently rot across months.
- Current guard: filter and summary behavior live in `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:18-63`; test uses current-month bounds in `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetUseCasesTest.kt`.

## DatePicker one-day timezone drift

- Symptom: Compose DatePicker can highlight/save adjacent day when local day-start millis are passed as UTC millis.
- Root cause: DatePicker uses UTC epoch dates; app stores local start-of-day millis.
- Resolution: convert local day start -> UTC for DatePicker input, then UTC -> local day start on confirm.
- Current guard: `DateUtils.toUtcDayStartMillis` and `toLocalDayStartMillis` bridge local day-start and DatePicker UTC millis: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtils.kt:80-90`; transaction form and sandbox sheet use them: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:343-356`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/sandbox/presentation/components/AddSandboxTransactionSheet.kt:331-343`.

## Calendar day visual-state conflicts

- Symptom: today with negative net rendered contradictory red border + green background (`bug-reports/calendar-view/2026-04-05_1300_today-cell-conflicting-visual-states.md:9-20`).
- Root cause: background and border were independent channels with no shared `isToday` x `netAmount` rule (`bug-reports/calendar-view/2026-04-05_1300_today-cell-conflicting-visual-states.md:44-68`).
- Resolution: today border always dark; backgrounds carry net tint; selected state wins. Fix plans: `docs/prompts/31_bugfix_calendar-day-today-cell-visual-state.md:9-17`, `docs/prompts/33_bugfix_calendar_non-today-cell-border-to-background.md:23-33`.
- Current guard: background resolver orders selected, today-negative, today, non-today positive/negative; border resolver only colors today: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt:106-132`.

## Overdue transactions must open edit mode, not create mode

- Symptom: tapping overdue row opened blank new-transaction form, so user could not confirm/cancel overdue item (`bug-reports/safe-to-spend/2026-04-01_overdue-transaction-cannot-be-resolved.md:9-25`).
- Root cause: Home screen did not track/pass selected transaction; `TransactionFormSheet` got `initialData = null` (`bug-reports/safe-to-spend/2026-04-01_overdue-transaction-cannot-be-resolved.md:31-80`).
- Resolution: track `editingTransaction`, pass it into form, and route save/delete to replace/delete. Fix plan: `docs/prompts/17_bugfix_overdue-transaction-cannot-be-resolved.md:15-23`.
- Current guard: Home stores editing state and row click sets it: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt:115-125`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt:264-273`; form receives `initialData` and replace/delete callbacks: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt:346-369`.

## Retired spec files are gone

- Symptom: old references to removed spec files may appear in historical prompts or bug reports.
- Root cause: the repo moved to `docs/engineering/` plus source code as the maintained truth.
- Resolution: use `docs/engineering/`, ADRs, and current code; treat `docs/prompts/` and `bug-reports/` as archaeology only.
- Current guard: actual package/entrypoints are Compose `App()` from Android and iOS: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:1`, `src/mobile/composeApp/src/androidMain/kotlin/com/petergabriel/budgetcalendar/MainActivity.kt:18-26`, `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`.
