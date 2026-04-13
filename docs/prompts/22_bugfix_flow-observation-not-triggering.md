# Bug Fix: Flow Observation Not Triggering — STS and Account Balances Don't Update

**Date:** 2026-04-01
**Bug Report:** `bug-reports/account-management/2026-04-01_1615_flow-observation-not-triggering.md`
**Feature:** Account Management + Transaction Management + Safe to Spend

---

## Tasks

- [ ] **Task 1: Add integration test to reproduce Flow observation bug** — Create a test using real `BudgetRepositoryImpl` (not FakeBudgetRepository) that: (1) gets initial STS value via `calculateSafeToSpendUseCase()`, (2) creates a PENDING expense via `CreateTransactionUseCase`, (3) verifies STS re-emits with updated value. Test should FAIL initially if Flow observation is broken. Place in `BudgetUseCasesTest.kt` near line 284. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 2: Diagnose SQLDelight Flow behavior** — Create a minimal test to verify if `asFlow()` re-emits on table changes. Check if `AndroidSqliteDriver` and `NativeSqliteDriver` properly notify observers, or if a different driver/approach is needed (e.g., `SqlDatabase.withTransaction` observation). Report findings before proceeding to Task 3. (Use Skill: kmp)

- [ ] **Task 3: Implement manual Flow trigger mechanism** — Add `MutableSharedFlow<Unit>` triggers to repositories. In `AccountRepositoryImpl`, `TransactionRepositoryImpl`, and `BudgetRepositoryImpl`, emit the trigger after every mutation (insert, update, delete). Then combine these triggers with existing Flows using `flow { emitAll(originalFlow) }.onStart { emit(Unit) }.distinctUntilChanged()`. This ensures Flows re-emit after data changes. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 4: Connect balanceChangedTrigger to Flow observation** — In `AccountRepositoryImpl`, the `balanceChangedTrigger` (line 30, 127) is emitted after `adjustBalance` but never collected. Modify `getAllAccounts()` and `getSpendingPoolAccounts()` to re-emit after balance changes by combining with `balanceChangedTrigger`. (Use Skill: kmp)

- [ ] **Task 5: Ensure CreateAccountUseCase triggers STS recalculation** — According to Account shadow spec (line 111): `If (isInSpendingPool) |> RecalculateSafeToSpend`. Verify `CreateAccountUseCase` triggers STS recalculation when a spending pool account is created, and ensure this propagates via Flow to UI. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 6: Run all integration tests and verify fix** — Run tests from Task 1 and existing balance-related tests. All must pass. Also verify manually: add account with $500 → STS=$500, add PENDING expense $75 → STS=$425, confirm → balance and STS update correctly. Commit with message: `fix: ensure SQLDelight Flows re-emit on data changes for STS and account updates` (Use Skill: kmp)
