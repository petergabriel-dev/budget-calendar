# Invariants

Must-not-break rules below are verified from current code only.

## Balance is ledger-derived

- Account balance = `initial_balance + SUM(signed_amount)` for confirmed transactions. Account reads use this formula: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:6-21`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:60-75`.
- Spending-pool total uses the same confirmed ledger formula: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:1-13`.
- Do not reintroduce mutable account-balance adjustment. Transaction writes store ledger rows and emit invalidation; they do not mutate an account balance column: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:127-173`.

## Safe-to-Spend formula

- STS = `pool balance - pending reservations - overdue reservations - credit-card reserved`, clamped `>= 0`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:45-50`.
- Confirmed spending is reported, but not subtracted again from STS; confirmed spending is already reflected in computed pool balance: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:40-63`.
- Pending/overdue reservations include spending-pool expenses only: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:15-22`.
- Credit-card reserved = sum of each card's projected unpaid balance, clamped per card before summing: `max(0, -(initial_balance + SUM(signed_amount) WHERE status != 'cancelled'))`. This includes pending/overdue/confirmed charges and payments; `statement_balance`/`due_date` and statement-cycle days do not feed STS: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-58`, `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:580-646`.
- Payment-sheet submissions create confirmed paired transfers. Confirmed pool-cash decrease and card-reserve release therefore leave final STS unchanged: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/MakeCreditCardPaymentUseCase.kt:36-48`, `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:684-732`.

## Credit-card displayed balance is projected

- Card displayed balance = `initial_balance + SUM(signed_amount WHERE status != 'cancelled')`; negative means owed, positive means overpaid credit. Query source: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/credit_card_settings.sq:27-46`.
- Credit-card summaries are transaction-reactive and must re-query through `transactionChangedTrigger.onStart { emit(Unit) }.flatMapLatest { ... }`, not `combine(trigger)`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummariesUseCase.kt:24-61`.
- Summary `currentBalance` is projected; `reservedAmount = max(0, -projected)`; `availableCredit = creditLimit - abs(projected)`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummariesUseCase.kt:36-56`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummaryByIdUseCase.kt:26-52`.
- Accounts credit-card tile leads with available credit when a limit exists, then shows owed + live statement due + utilization; it must not replace section-total/net-worth math: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/CreditCardTile.kt:38-52`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/CreditCardTile.kt:95-130`.
- Statement due is live display-only: `max(0, -(balance as of last close) - payments since close)` with due date from `paymentDueAfter`; null when cycle days are missing. It is distinct from total owed/current balance and does not feed STS or net worth: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummariesUseCase.kt:64-72`.
- Net worth = non-card confirmed balances + signed projected card balances. Pending card charges lower net worth; pending spending-pool expenses do not until confirmed: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:124-128`.

## Status lifecycle and overdue marking

- Valid transitions: `PENDING -> OVERDUE | CONFIRMED | CANCELLED`; `OVERDUE -> CONFIRMED | CANCELLED`; `CONFIRMED` and `CANCELLED` are terminal: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt:31-40`.
- Linked transfer rows are status-updated together when `linkedTransactionId` is present: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt:21-28`.
- Pending becomes overdue only when `nowMillis > transaction.date + DateUtils.MILLIS_IN_DAY`, and the overdue-marking query excludes credit-card accounts so card charges stay out of the overdue lifecycle: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq:25-31`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:182-205`.
- Overdue marking is invoked through `MarkOverdueTransactionsUseCase`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/MarkOverdueTransactionsUseCase.kt:6-11`.

## App lifecycle recompute sites

- `App()` injects overdue, projection, STS, settings, and notification use cases: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:72-82`.
- Initial launch runs `refreshNotifications(...)` from `LaunchedEffect(Unit)`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:84-95`.
- Later `ON_RESUME` events also run `refreshNotifications(...)` after the first resume is skipped: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:97-120`.
- `refreshNotifications` first marks overdue, then reads overdue transactions, projection, STS, settings, and generates notifications: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:196-226`.

## Credit cards require inline setup and never enter the spending pool

- Creating a credit-card account requires credit limit `> 0`, statement closing day 1-31, and payment due day 1-31 before account creation proceeds; card settings are written after account insert and rollback deletes the account if settings fail: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:23-61`.
- Add-account UI labels card balance as "Current balance owed", collects required limit/closing/due fields, and stores owed as negative `initialBalance`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountFormSheet.kt:159-219`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountScreen.kt:151-159`.
- Credit-card account creation forces `isInSpendingPool = false`, even if the request asks for true: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:40-47`.
- Updating an account to or as `credit_card` also forces `isInSpendingPool = false`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/UpdateAccountUseCase.kt:22-32`.

## Transaction sheet treats credit-card expenses as card charges

- When the selected source account is a credit card, the form collapses type to EXPENSE/charge, hides the Expense/Income/Transfer segmented control, clears destination, and saves `type = EXPENSE`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:133-145`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:241-263`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:535-542`.
- For new credit-card charges, same-day/past charges default to confirmed and the status label reads "Posted"; future charges default to pending/scheduled. Cash/bank/savings keep "Paid / Cleared": `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:137-146`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:420-435`, `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheetLogicTest.kt:37-56`.
- The statement-context line is display-only, shown only when both cycle days are set, and computes from charge date via `DateUtils`; it does not affect ledger/STS: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:449-455`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:657-667`.
- The transaction form suppresses the balance-based insufficient-funds warning for credit-card source accounts; bank/cash/savings accounts keep the warning path: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:185-189`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:440-447`.
- For credit-card EXPENSE rows with a card summary, the sheet shows available credit when present, balance after charge, and Safe-to-Spend reserve impact: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:458-480`.
- When a credit limit exists and the charge exceeds available credit, the sheet shows error text, blocks click-time save, and disables Save: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/model/CreditCardChargeImpact.kt:11-26`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:525-550`.

## Calendar transaction interaction

- Calendar transaction rows, inline and expanded, open read-only `TransactionDetailsSheet`; they do not open `TransactionFormSheet` directly: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:69-81`, `:142-152`, `:193-202`, `:230-244`.
- Calendar row gestures remain tap-only. Do not reintroduce `combinedClickable`, a long-press callback, or long-press deletion: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionListItem.kt:27-33`, `:65-73`; `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/DayTransactionList.kt:20-59`.
- Calendar Edit and Delete are reachable only through details overflow. Edit opens the prefilled form; Delete must require confirmation, and Cancel performs no delete: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionDetailsSheet.kt:100-126`, `:180-211`.

## Calendar grid is fixed and non-scrollable

- Calendar month construction always emits 42 contiguous dates, beginning Sunday on or before month start. Exactly target-month days have `isCurrentMonth = true`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/BuildCalendarMonthUseCase.kt:48-65`; regression coverage: `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/BuildCalendarMonthUseCaseTest.kt:18-57`.
- Calendar UI renders six weighted rows with weighted, full-height cells; no calendar lazy layout or scroll state is allowed: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarGrid.kt:45-60`.
- Parent layouts must allocate calendar from leftover space; do not hardcode day-cell height. `CalendarScreen` weights calendar region and grid: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:111-114`, `:130-133`.

## Transfers are paired rows

- Creating a transfer writes a source row with `signedAmount = -amount`, a destination row with `signedAmount = amount`, then links both IDs: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:59-82`.
- Deleting a transfer deletes the linked row first when found, then deletes the requested row: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/DeleteTransactionUseCase.kt:9-21`.
- Transfer source and destination account must differ: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:17-24`.

## Cancelled and sandbox rows stay out of real calculations

- Main `transactions` has no sandbox flag. Sandbox transactions live only in `sandbox_transactions`, so real account/budget queries need no `is_sandbox` filter: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:14-28`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:71-86`.
- Real account balances include only `status = 'confirmed'`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:16-20`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:70-74`.
- Real budget totals/reservations include only relevant statuses/types: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:8-11`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:15-22`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-58`.
- Because real calculations filter to `confirmed`, `pending/overdue`, or `status != 'cancelled'`, `cancelled` rows are excluded from balances and reservations by status filters above.

## Input validation at transaction boundary

- Amount must be `> 0`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:12-15`.
- Transfer requires destination account and source != destination: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:17-24`.
- Income cannot be in the past: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:26-30`.
- Expense cannot be scheduled more than 30 days ahead: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:32-36`.
