# Bug Fix: Confirmed Transaction Doesn't Update Account Balance or STS

**Date:** 2026-04-02
**Bug Reports:**
- `bug-reports/account-management/2026-04-01_1600_transaction-not-affecting-account-at-all.md`
- `bug-reports/account-management/2026-04-01_1615_flow-observation-not-triggering.md`
**Feature:** Transaction Management + Account Management + Safe to Spend

**Root Causes (two compounding bugs):**
1. `CreateTransactionUseCase` never calls `accountRepository.adjustBalance()` when a transaction is created with `status = CONFIRMED`. Balance adjustment only lived in `UpdateTransactionStatusUseCase` (status transitions), leaving a gap for create-as-confirmed.
2. `BudgetRepositoryImpl` and `AccountRepositoryImpl` use `combine(trigger)` to re-emit after mutations, but `combine` replays the last emitted value — it does NOT re-execute the underlying SQLDelight query. The correct operator is `flatMapLatest`, which cancels the current query and starts a fresh one each time the trigger fires.

---

## Tasks

- [ ] **Task 1: Add reproduction integration test for create-as-confirmed** — In `TransactionUseCasesTest.kt` alongside existing balance tests (near line 331), add a test `createConfirmedExpense_deductsAccountBalance` that: (1) creates an account with $500 balance using real `AccountRepositoryImpl` and `TransactionRepositoryImpl`, (2) calls `CreateTransactionUseCase` with `status = CONFIRMED` and `amount = 7500` (cents), (3) asserts the account balance is now $425_00, (4) also asserts STS reflects the change. This test MUST FAIL before the fixes are applied. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 2: Fix `BudgetRepositoryImpl` — replace `combine` with `flatMapLatest`** — In `BudgetRepositoryImpl.kt`, the `transactionChangedTrigger` local val already uses `.onStart { emit(Unit) }`. For every method (`getTotalSpendingPoolBalance`, `getPendingReservations`, `getOverdueReservations`, `getCreditCardReservedAmount`, `getCreditCardReservations`), replace the `combine(transactionChangedTrigger)` pattern with `transactionChangedTrigger.flatMapLatest { <sqlDelightQueryFlow> }`. The SQLDelight query and its `.asFlow().mapTo*()` chain moves inside the `flatMapLatest` lambda. This causes a fresh database query on every trigger emission instead of re-emitting the stale cached value. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 3: Fix `AccountRepositoryImpl` — replace `combine` with `flatMapLatest`** — In `AccountRepositoryImpl.kt`, for `getAllAccounts()` and `getSpendingPoolAccounts()`, replace the `.combine(balanceChangedTrigger.onStart { emit(Unit) }) { accounts, _ -> accounts }` terminal pattern with `balanceChangedTrigger.onStart { emit(Unit) }.flatMapLatest { <sqlDelightQueryFlow> }` wrapping the full SQLDelight chain. Same rationale as Task 2 — `flatMapLatest` re-executes the query; `combine` does not. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 4: Fix `CreateTransactionUseCase` — call `adjustBalance()` for CONFIRMED creates** — Add `IAccountRepository` as a constructor dependency to `CreateTransactionUseCase`. After `transactionRepository.createTransaction(request)` succeeds, check `if (request.status == TransactionStatus.CONFIRMED)` and call `accountRepository.adjustBalance()` with the correct signed delta — mirror the logic in `UpdateTransactionStatusUseCase` for determining direction (EXPENSE = negative delta, INCOME = positive, TRANSFER = debit source and credit destination). For TRANSFER types, both the source and destination adjustments must happen. Update `KoinModules.kt` to inject `IAccountRepository` into `CreateTransactionUseCase`. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 5: Update Transaction shadow spec — document create-as-confirmed path** — In `specs/shadow/Transaction.shadow.md`, update the "Create Transaction Flow" (lines 126–131) to explicitly cover the `status == CONFIRMED` case. Add a branch: `|> If (status == CONFIRMED) |> AccountRepository.adjustBalance()` after `TransactionRepository.insert()`. This closes the spec gap that only documented balance adjustment in the "Confirm Transaction Flow". (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 6: Run all tests and verify the full flow** — Run `./gradlew :composeApp:testDebugUnitTest` from `src/mobile/`. The reproduction test from Task 1 must now pass. All pre-existing balance and STS tests must continue to pass. Verify manually on device/emulator: (1) add spending pool account $500 → STS = $500, (2) add PENDING expense $75 → STS = $425, (3) confirm the expense → account balance = $425, STS stays $425. Commit with message: `fix: adjustBalance on confirmed-at-creation transactions and flatMapLatest for reactive repo flows` (Use Skill: kmp)
