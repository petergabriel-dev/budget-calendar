# Bug Fix: Flow Observation Not Triggering — STS and Account Balances Don't Update

**Date:** 2026-04-01
**Bug Report:** `bug-reports/account-management/2026-04-01_1615_flow-observation-not-triggering.md`
**Feature:** Account Management + Transaction Management + Safe to Spend
**Root Cause:** SQLDelight drivers don't auto-notify observers; `balanceChangedTrigger` exists but is never collected

---

## Tasks

- [ ] **Task 1: Add integration test to reproduce Flow observation bug** — Create a test using real `BudgetRepositoryImpl` and `TransactionRepositoryImpl` (not fakes) that: (1) gets initial STS via `calculateSafeToSpendUseCase()`, (2) creates a PENDING expense via `CreateTransactionUseCase`, (3) verifies STS Flow re-emits with updated pending deduction. Test should FAIL initially since Flows don't re-emit. Place in `BudgetUseCasesTest.kt` near line 284 alongside existing bugfix tests. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 2: Connect balanceChangedTrigger to AccountRepositoryImpl Flows** — Modify `AccountRepositoryImpl.getAllAccounts()` (line 32-38) and `getSpendingPoolAccounts()` (line 47-53) to combine with `balanceChangedTrigger` using `.combine(balanceChangedTrigger) { accounts, _ -> accounts }`. This ensures Flows re-emit after any mutation (create, update, delete, adjustBalance). Add `.onStart { emit(Unit) }` before combine to emit immediately on subscription. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 3: Add transactionChangedTrigger to TransactionRepositoryImpl** — Add a `MutableSharedFlow<Unit>(replay = 1)` and expose it as `transactionChangedTrigger: SharedFlow<Unit>`. Emit after `createTransaction()` (line 138), `updateTransactionStatus()` (line 144-150), and `deleteTransaction()` (line 162). This allows other repositories (BudgetRepositoryImpl) to react to transaction changes. (Use Skill: kmp)

- [ ] **Task 4: Connect transactionChangedTrigger to BudgetRepositoryImpl Flows** — Modify `BudgetRepositoryImpl` to accept `ITransactionRepository` as a dependency and combine all budget Flows with `transactionRepository.transactionChangedTrigger`. This ensures STS recalculates when transactions are created/modified. Use `.combine(transactionChangedTrigger) { data, _ -> data }` pattern on `getPendingReservations()`, `getOverdueReservations()`, `getTotalSpendingPoolBalance()`. (Use Skill: kmp)

- [ ] **Task 5: Wire up transactionChangedTrigger in KoinModules** — Update `KoinModules.kt` to pass `TransactionRepositoryImpl` to `BudgetRepositoryImpl` constructor. Currently line 107: `single { BudgetRepositoryImpl(get(), get()) }` needs to become `single { BudgetRepositoryImpl(get(), get(), get()) }` where the third `get()` is the transaction repository. (Use Skill: kmp)

- [ ] **Task 6: Run integration tests and verify fix** — Run test from Task 1 and all existing balance-related tests. Verify manually: (1) add account with $500 → STS=$500, (2) add PENDING expense $75 → STS=$425, (3) confirm expense → balance and STS update correctly. Commit with message: `fix: connect Flow triggers so STS and account balances update on transaction changes` (Use Skill: kmp)
