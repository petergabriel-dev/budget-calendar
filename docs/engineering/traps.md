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

## Retired credit-card reservation trap: confirmed card charge released reserve

- Symptom: STS over-reported after a credit-card charge moved from `pending` to `confirmed` because the reservation disappeared while the card debt still existed.
- Root cause: old reserve SQL counted only pending/overdue card expenses, not the card's full liability balance or payments.
- Resolution: credit-card reserve now uses projected unpaid balance per card: `max(0, -(initial_balance + SUM(signed_amount) WHERE status != 'cancelled'))`; the total query sums the per-card clamp: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-58`.
- Current guard: cards are never in the spending pool, so card debt reaches STS only through the reserve: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:23-31`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/UpdateAccountUseCase.kt:22-32`.

## Pending card payments release STS early

- Symptom: submitting a card payment could increase STS before source cash was confirmed as spent.
- Root cause: card reserve includes non-cancelled destination payment rows, but pool balance includes only confirmed source rows. A pending payment releases reserve without lowering pool cash.
- Resolution: payment-sheet submissions create confirmed paired transfers; source cash decrease and reserve release reach final ledger state together.
- Current guard: `MakeCreditCardPaymentUseCase` sets `TransactionStatus.CONFIRMED`, while SQLite integration asserts $1,000 cash plus $120 card debt becomes $880 cash, $0 reserve, and $880 STS: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/MakeCreditCardPaymentUseCase.kt:36-48`, `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:684-732`.

## Stale credit-card summaries from settings-only flows

- Symptom: Accounts showed a card section total of `₱0.00` while the card detail sheet showed real card debt after transaction writes.
- Root cause: summary flow observed credit-card settings only; transaction writes did not cause projected card balances to re-query. Using `combine(trigger)` can still replay stale query values.
- Resolution: wrap summary query work in `transactionChangedTrigger.onStart { emit(Unit) }.flatMapLatest { ... }`, then batch projected balances with `getAllProjectedBalances()` and derive displayed balance/reserve/available credit from projected: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummariesUseCase.kt:24-61`.
- Current guard: projected SQL includes non-cancelled pending/overdue/confirmed rows, and net worth uses signed projected card balances: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/credit_card_settings.sq:27-46`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:124-128`.

## Credit-card transactions are not cash withdrawals

- Symptom: adding/editing a credit-card expense could show "Insufficient funds" against the card balance, which is meaningless for a liability account.
- Root cause: `TransactionFormSheet` reused the cash-account balance warning for all EXPENSE/TRANSFER sources.
- Resolution: credit-card source accounts skip the cash insufficient-funds warning; credit-card charges are forced to EXPENSE, use Posted/Scheduled status wording, and hard-block over-limit saves only when a credit limit is set.
- Current guard: card-aware type/status derivation, visible statement/credit context, click-time save guard, and disabled Save live in `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:133-145`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:420-480`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:525-550`.

## Retired manual credit-card statement fields

- Symptom: old settings UI may tempt reintroducing manual `statement_balance` / `due_date` edits as if they drive STS or payment timing.
- Root cause: those columns still exist for migration safety, but recurring `statement_closing_day` / `payment_due_day` are now the UI source for statement-cycle display.
- Resolution: keep `statement_balance` and `due_date` columns, but do not expose manual inputs; pass existing values through on save instead of nulling them.
- Current guard: settings form shows Closing day + Due day, validates 1-31, and passes vestigial values from `initialSettings`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardSettingsForm.kt:83-163`; detail sheet shows live statement due plus computed next due date, not manual statement rows: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardDetailSheet.kt:58-86`.

## Credit-card closing boundary is exclusive

- Symptom: a charge dated on closing day can be shown on the already-closing statement if helper uses `>=` for the next close.
- Root cause: inclusive boundary treats closing day as still before/on current cycle for charge display.
- Resolution: charge cutoff is exclusive: charge on closing day bills to next statement. Use `statementClosingOnOrAfter` for charge display and `lastStatementClose` for live statement due: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtils.kt:92-115`.
- Current guard: tests cover closing-day rollover, last-close boundaries, and February/leap clamp: `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtilsStatementCycleTest.kt`.

## Legacy credit cards may lack setup rows or cycle days

- Symptom: existing cards from before mandatory setup can lack limit or cycle, causing tile/detail statement fields to look incomplete or crash if assumed non-null.
- Root cause: setup is mandatory only for new account creation; backfill is out of scope.
- Resolution: render missing limit/cycle gracefully: available credit/statement due/due date become "Not set" or null; no crash, no fallback ledger mutation.
- Current guard: statement due returns null without both cycle days and tile avoids utilization without limit: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummariesUseCase.kt:64-72`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/CreditCardTile.kt:39-48`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/CreditCardTile.kt:115-130`.

## Current card owed is stored as negative initial balance

- Symptom: double-negating or storing card owed as positive makes net worth, available credit, and reserve wrong.
- Root cause: credit cards use signed projected balance; negative means owed, positive means overpaid.
- Resolution: add-card UI accepts "Current balance owed" as positive input once, then `AccountScreen` negates it for `CreateAccountRequest.initialBalance`.
- Current guard: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountScreen.kt:151-159` and domain still permits negative card balances while rejecting negative non-card balances: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:19-20`.

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

## Calendar grid height drift created hidden lazy-grid scroll

- Symptom: August 2026 lost its last row on first paint; navigating Aug → Jul → Aug left the grid clipped under weekday labels.
- Root cause: parent `CalendarGrid` duplicated a 66.dp day-cell height while `CalendarDayCell` rendered at least 80.dp including padding. Overflow gave its `LazyVerticalGrid` a scroll range; `userScrollEnabled = false` blocks gestures, not lazy-state restoration by item key.
- Resolution: build 42 dates and render six weighted `Row`s in a plain `Column`; parent and grid divide leftover height instead of sharing a cell-height constant.
- Current guard: `BuildCalendarMonthUseCase` fixes `totalCells = 42`, and `CalendarGrid` has weighted rows/cells with no lazy layout: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/BuildCalendarMonthUseCase.kt:48-56`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarGrid.kt:45-60`. `CalendarScreen` weights both calendar region and grid: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:111-114`, `:130-133`.

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
