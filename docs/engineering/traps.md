# Traps

Current-code guards for bugs this project has already hit. `docs/engineering/` and code are authoritative.

## Double window insets shift screen headers

- Symptom: Calendar and Accounts headers sat roughly one status-bar height below Home and Settings.
- Root cause: Calendar added `safeDrawingPadding()` after root `Scaffold` padding; Accounts' nested snackbar `Scaffold` consumed system bars again.
- Resolution: root `App.kt` `Scaffold` owns system insets; Calendar only consumes passed padding, and Accounts sets nested `contentWindowInsets = WindowInsets(0, 0, 0, 0)`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:129-179`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:105-110`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountScreen.kt:64-72`.

## Unweighted headers hide actions

- Symptom: long header titles could push bell/action controls off-screen.
- Root cause: title text had no width constraint inside header row.
- Resolution: keep `BcHeader` title weighted, single-line, and ellipsized; retain explicit gap before action content: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcHeader.kt:35-56`.

## Nested header controls raise the bell

- Symptom: Calendar bell sat about 2dp high relative to bells on other tabs.
- Root cause: a nested `Row` grouped 48dp month buttons with the 44dp bell but omitted `verticalAlignment`, which defaults to top alignment.
- Resolution: pass `BcNotificationBell` directly to `BcHeader.actionContent`; render month controls in sibling `MonthNavigationRow`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:117-132`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/MonthNavigationRow.kt:25-63`.

## Centered wrap-content Columns stay at start

- Symptom: empty-state content drifts toward start despite `horizontalAlignment = Alignment.CenterHorizontally`.
- Root cause: `Modifier.weight(1f)` from a `Column` parent constrains height only, so child Column wraps its widest element and has no horizontal space to center within.
- Resolution: apply `fillMaxWidth()` before padding on centered child Column; pass centered alignment to nested layout that must align label with amount.
- Current guard: Accounts parent still passes height-only weight: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountScreen.kt:91-96`; `EmptyState` fills width and centers `NetWorthHero`, whose default remains start for populated list: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountListScreen.kt:164-175`, `:201-211`.

## Mutable balance drift

- Symptom: confirmed transactions existed but account balance stayed stale (`bug-reports/account-management/2026-04-02_1200_stored-balance-permanently-drifts-from-transaction-history.md:11-30`).
- Root cause: old mutable `accounts.balance` depended on every write path calling `adjustBalance()`.
- Resolution: migration 7 renamed balance to `initial_balance` and added/backfilled `signed_amount`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/7.sqm:1-23`.
- Current guard: balance reads compute `initial_balance + SUM(signed_amount)` for confirmed rows; sandbox transactions live in `sandbox_transactions`, not main `transactions`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:60-75`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:71-86`.

## Migration tests must match generated SQLDelight query APIs

- Symptom: migration test does not compile or invokes a generated query as if it were a direct function.
- Root cause: generated query names and result APIs come from the current `.sq` definitions; assumed names/signatures can be stale, especially for sandbox and foreign-key queries.
- Resolution: inspect generated query sources or current `.sq` definitions, use the exact generated method name, and call `executeAsList()`/other result APIs on the returned query object as required: `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:504-518`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/sandbox_transactions.sq:1-80`.

## Migration fixture failures need exact failing phase

- Symptom: a migration test reports `SQLiteException` at the test method line, appearing to implicate fixture creation or a direct insert.
- Root cause: `runBlocking` stack traces point to the test method even when the exception occurs later during schema migration; migration setup can also collide with the current schema version.
- Resolution: inspect the XML test report and generated migration code to identify the failing SQL and phase before changing fixture inserts. For a v12-to-v13 migration against the current fixture, synthesize only the historical v12 schema differences before invoking migration: `src/mobile/composeApp/build/generated/sqldelight/code/BudgetCalendarDatabase/commonMain/com/petergabriel/budgetcalendar/core/database/composeApp/BudgetCalendarDatabaseImpl.kt:470-530`, `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:531-532,952-979`.

## SQLDelight flows do not refresh dependent repos by magic

- Symptom: STS/account UI did not update after transaction writes (`bug-reports/account-management/2026-04-01_1615_flow-observation-not-triggering.md:9-29`).
- Root cause: dependent SQLDelight `.asFlow()` queries emitted once or replayed stale values; `combine(trigger)` did not force re-query.
- Resolution: `transactionChangedTrigger` + `flatMapLatest` around SQLDelight query chains.
- Current guard: transaction writes emit trigger: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:103-181`; account/budget repos re-query via `flatMapLatest`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:31-62`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/data/repository/BudgetRepositoryImpl.kt:27-75`.

## Overloaded account flags hide distinct business questions

- Symptom: one boolean causes Safe-to-Spend eligibility and valid money-source behavior to drift, often hidden because forms default the value to `true`.
- Root cause: one overloaded flag answered two unrelated questions.
- Resolution: derive Safe-to-Spend eligibility from `AccountType`; make each call site select the explicit question it asks. Pool reads use type-derived membership, while card payments filter valid sources separately: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/Account.kt:3-30`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/CreditCardViewModel.kt:265-279`.

## Spending-pool membership must follow account type

- Symptom: Safe-to-Spend includes savings balances or payment-source selection excludes savings accounts.
- Root cause: treating spending-pool membership and payment-source eligibility as the same question, or restoring a mutable per-account override after membership became type-derived.
- Current guard: `AccountType` marks only `CHECKING`/`CASH` as pool members; budget SQL repeats `a.type IN ('checking', 'cash')` for pool totals and pending/overdue reservations. Credit-card payments observe all accounts and exclude only `CREDIT_CARD`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/Account.kt:3-30`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:1-22`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq:88-109`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/CreditCardViewModel.kt:265-279`.

## Positional SQLDelight mapper follows projection order

- Symptom: account data maps to wrong fields or fails compile after an account query projection changes.
- Root cause: SQLDelight mapper callbacks are positional; `AccountRepositoryImpl.toEntity` must match `accounts.sq` column order exactly.
- Current guard: account projections and `toEntity` both contain `id`, `name`, `type`, `balance`, `description`, `created_at`, and `updated_at`; migration 13 rebuilds `accounts` with the same surviving columns after removing the stored pool flag: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:1-47`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:124-142`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/13.sqm:3-18`.

## Confirmed spending double-deduction

- Symptom: after confirming a pending expense, STS changed incorrectly (`bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md:9-18`).
- Root cause: confirmed expenses are already reflected in balance; subtracting them again double-counts them (`bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md:70-89`).
- Resolution: keep STS formula as pool balance minus pending/overdue/CC reserved; confirmed expenses are already included in pool balance, so never subtract them twice.
- Current guard: `BudgetSummary` has no separate confirmed-spending field; the confirmed-only pool query already includes those expenses, while STS subtracts only pending, overdue, and card reserve: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/model/BudgetSummary.kt:3-10`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:1-13`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:18-27`.

## Retired credit-card reservation trap: confirmed card charge released reserve

- Symptom: STS over-reported after a credit-card charge moved from `pending` to `confirmed` because the reservation disappeared while the card debt still existed.
- Root cause: old reserve SQL counted only pending/overdue card expenses, not the card's full liability balance or payments.
- Resolution: credit-card reserve now uses projected unpaid balance per card: `max(0, -(initial_balance + SUM(signed_amount) WHERE status != 'cancelled'))`; the total query sums the per-card clamp: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-39`.
- Current guard: cards are never in the spending pool because `AccountType.CREDIT_CARD.isInSpendingPool` is `false`, so card debt reaches STS only through the reserve: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/Account.kt:3-11`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-39`.

## Pending card payments release STS early

- Symptom: submitting a card payment could increase STS before source cash was confirmed as spent.
- Root cause: card reserve includes non-cancelled destination payment rows, but pool balance includes only confirmed source rows. A pending payment releases reserve without lowering pool cash.
- Resolution: payment-sheet submissions create confirmed paired transfers; source cash decrease and reserve release reach final ledger state together.
- Current guard: `MakeCreditCardPaymentUseCase` sets `TransactionStatus.CONFIRMED`, while SQLite integration asserts $1,000 cash plus $120 card debt becomes $880 cash, $0 reserve, and $880 STS: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/MakeCreditCardPaymentUseCase.kt:36-48`, `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:804-950`.

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



## DatePicker one-day timezone drift

- Symptom: Compose DatePicker can highlight/save adjacent day when local day-start millis are passed as UTC millis.
- Root cause: DatePicker uses UTC epoch dates; app stores local start-of-day millis.
- Resolution: convert local day start -> UTC for DatePicker input, then UTC -> local day start on confirm.
- Current guard: `DateUtils.toUtcDayStartMillis` and `toLocalDayStartMillis` bridge local day-start and DatePicker UTC millis: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtils.kt:80-90`; transaction form and sandbox sheet use them: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:343-356`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/sandbox/presentation/components/AddSandboxTransactionSheet.kt:331-343`.

## Month-scoped calendar cache loses off-month selection rows

- Symptom: day panel reports no transactions for selected day after displayed month changes.
- Root cause: `GetMonthTransactionsUseCase` returns only requested month, so displayed month's `transactionsByDate[selectedDate]` lacks off-month rows: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/GetMonthTransactionsUseCase.kt:15-30`.
- Resolution: keep selection independent of displayed month. Cancel old selected-day job on each load; when selection is off-month, collect selected month and resolve through `BuildCalendarMonthUseCase`.
- Current guard: `CalendarViewModel` owns job cancellation and off-month resolution: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarViewModel.kt:43-100`, `:138-144`, `:164-196`; regression coverage includes liveness and cancelled exclusion: `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarViewModelTest.kt:48-108`.

## Calendar day visual-state conflicts

- Symptom: today with negative net rendered contradictory red border + green background (`bug-reports/calendar-view/2026-04-05_1300_today-cell-conflicting-visual-states.md:9-20`).
- Root cause: background and border were independent channels with no shared `isToday` x `netAmount` rule (`bug-reports/calendar-view/2026-04-05_1300_today-cell-conflicting-visual-states.md:44-68`).
- Resolution: today border always dark; backgrounds carry net tint; selected state wins.
- Current guard: background resolver orders selected, today-negative, today, non-today positive/negative; border resolver only colors today: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarDayCell.kt:106-132`.

## Calendar grid height drift created hidden lazy-grid scroll

- Symptom: August 2026 lost its last row on first paint; navigating Aug → Jul → Aug left the grid clipped under weekday labels.
- Root cause: parent `CalendarGrid` duplicated a 66.dp day-cell height while `CalendarDayCell` rendered at least 80.dp including padding. Overflow gave its `LazyVerticalGrid` a scroll range; `userScrollEnabled = false` blocks gestures, not lazy-state restoration by item key.
- Resolution: build 42 dates and render six weighted `Row`s in a plain `Column`; parent and grid divide leftover height instead of sharing a cell-height constant.
- Current guard: `BuildCalendarMonthUseCase` fixes `totalCells = 42`, and `CalendarGrid` has weighted rows/cells with no lazy layout: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/BuildCalendarMonthUseCase.kt:48-56`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/components/CalendarGrid.kt:45-60`. `CalendarScreen` weights both calendar region and grid: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarScreen.kt:112-115`, `:142-145`.

## Overdue transactions must open edit mode, not create mode

- Symptom: tapping overdue row opened blank new-transaction form, so user could not confirm/cancel overdue item (`bug-reports/safe-to-spend/2026-04-01_overdue-transaction-cannot-be-resolved.md:9-25`).
- Root cause: Home screen did not track/pass selected transaction; `TransactionFormSheet` got `initialData = null` (`bug-reports/safe-to-spend/2026-04-01_overdue-transaction-cannot-be-resolved.md:31-80`).
- Resolution: track `editingTransaction`, pass it into form, and route save/delete to replace/delete.
- Current guard: Home stores editing state and row click sets it: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt:116-125`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt:264-273`; form receives `initialData` and replace/delete callbacks: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt:346-369`.

## Shared sheet mutation can close too early

- Symptom: forms disappear immediately after submit, so failed writes lose entered values and context.
- Root cause: callers treated async mutation invocation as success instead of observing completion.
- Resolution: feature ViewModels own busy/error/success state; `BcSheetShell` owns layout only. Close on success effect/callback, preserve values on failure, and guard dirty dismissal with `BcConfirmDialog`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcSheetShell.kt:33-54`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/TransactionFormViewModel.kt:150-220`.

## Broad Sandbox errors blank usable content

- Symptom: one failed snapshot, projection, or transaction read hides unrelated Sandbox content.
- Root cause: one broad loading/error field represented multiple independent operations.
- Resolution: keep snapshot, projection, transaction, and mutation fields separate; preserve last usable metric/list and expose retry beside affected content: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/sandbox/presentation/SandboxHomeUiState.kt:3-29`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/sandbox/presentation/SandboxViewModel.kt:165-344`.

## Retired spec files are gone

- Symptom: old references to removed spec files may appear in historical prompts or bug reports.
- Root cause: the repo moved to `docs/engineering/` plus source code as the maintained truth.
- Resolution: use `docs/engineering/`, ADRs, and current code; treat retired prompt/bug-report archives as archaeology only.
- Current guard: actual package/entrypoints are Compose `App()` from Android and iOS: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:1`, `src/mobile/composeApp/src/androidMain/kotlin/com/petergabriel/budgetcalendar/MainActivity.kt:18-26`, `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`.

## Preview CSS can be valid but invisible

- Symptom: a design preview screenshot shows browser defaults even though its CSS has balanced braces and one style block.
- Root cause: the preview was served or reloaded without applying its inline stylesheet.
- Resolution: inspect serving/reload and stylesheet loading before changing tokens. Keep metadata outside the phone frame; frame actual app surfaces only. Use theme `bg-primary` for the stage, explicit full tokenized row borders, and tokenized section gaps. Declare every referenced primitive, including `--p-space-5` and `--p-blue-100`, and verify light info text mapping.
- Current guard: six files under `docs/design/previews/` receive structural CSS/HTML checks before screenshot review.

## Transparent button containers must not receive disabled alpha copies

- Symptom: disabled `Outline`, `OutlineIconTrailing`, or `Ghost` buttons rendered an opaque black container, making labels unreadable.
- Root cause: copying disabled alpha onto `Color.Transparent` replaces its zero-alpha value with an opaque color.
- Resolution: `resolveButtonContainerColor` returns transparent colors unchanged and dims only solid containers in `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcButton.kt:78-79`, `:131-145`.
- Current guard: all six `ButtonVariant` values are checked in enabled and disabled states against both light and dark palettes: `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcButtonTest.kt:12-64`.

## Compose test assertions can mismatch rendered semantics

- Symptom: a test cannot find an expected label or uses unavailable collection helpers.
- Root cause: rendered text may be transformed (`BcBadge` renders `PROMOTED`), UI copy may be uppercase (`TOTAL NET WORTH`), and this Compose test dependency does not provide `onLast()`/`onAllNodes` in every context.
- Resolution: assert canonical rendered text and use stable semantic nodes such as `onNodeWithText(...)`; inspect dependency APIs before adding collection helpers. Keep required extensions such as `performClick` imported.
- Current guard: Sandbox and app-shell instrumented tests assert rendered labels and semantic content descriptions: `src/mobile/composeApp/src/androidInstrumentedTest/kotlin/com/petergabriel/budgetcalendar/features/sandbox/presentation/components/SandboxComponentsTest.kt`, `src/mobile/composeApp/src/androidInstrumentedTest/kotlin/com/petergabriel/budgetcalendar/AppShellIntegrationTest.kt`.

## Compose refactors can fail at brace boundaries

- Symptom: a sheet-shell refactor reports a syntax error near the body/footer boundary.
- Root cause: one extra closing brace changed the composable nesting rather than the feature logic.
- Resolution: inspect the exact surrounding lines, remove only the unmatched brace, then rerun the narrow compile before interpreting downstream failures.

## Mutation exceptions can escape Result APIs

- Symptom: a repository exception escapes a use-case call and the sheet never receives its scoped error.
- Root cause: tests/fakes may throw directly even when production use cases return `Result`.
- Resolution: UI mutation wrappers convert non-cancellation throwables to `Result.failure`; `CancellationException` is rethrown so coroutine cancellation remains correct. `TransactionFormViewModel.performMutation` is the reference pattern: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/TransactionFormViewModel.kt:220-231`.

## Full-suite failures need baseline separation

- Symptom: a targeted task check passes while `allTests` reports a race or unrelated existing failure.
- Root cause: full-suite failures may predate the task and can be confused with newly changed code.
- Resolution: run and record baseline before source edits, compare the exact failing tests, fix only attributable regressions, and do not commit or tick a blocked task until the gate is resolved or explicitly documented.

## Missing presentation tests hide feature gaps

- Symptom: domain tests pass while requested settings or notification UI coverage is absent.
- Root cause: test discovery stopped after `commonTest` and missed presentation sources or `androidInstrumentedTest`.
- Resolution: inventory feature presentation files and all three test source sets before claiming coverage; add the smallest semantic/component tests needed for the requested states.
