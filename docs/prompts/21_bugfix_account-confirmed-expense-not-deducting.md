# Bug Fix: Confirmed Expense Not Deducting from Account Balance

**Date:** 2026-04-01
**Bug Report:** `bug-reports/account-management/2026-04-01_1515_confirmed-expense-not-deducting-from-account-balance.md`
**Feature:** Account Management + Transaction Management

---

## Tasks

- [ ] **Task 1: Add integration test to reproduce the confirmed expense balance bug** — Create a test that verifies: (1) creates an account with $500 initial balance, (2) creates a PENDING expense for $75, (3) confirms the expense, (4) asserts account balance is $425 after confirmation. Place test in `TransactionUseCasesTest.kt` alongside existing balance tests (around line 331). This test should FAIL initially if the bug exists, then PASS after the fix. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 2: Verify `adjustBalance` SQL query execution** — Run the app and manually confirm a transaction, then query the database directly: `SELECT balance FROM accounts WHERE id = ?`. If balance is unchanged at $500, add logging to `AccountRepositoryImpl.adjustBalance()` to trace whether the SQL UPDATE executes. Report findings before proceeding to Task 3. (Use Skill: kmp)

- [ ] **Task 3: Fix STS formula double-deduction** — In `CalculateSafeToSpendUseCase.kt` lines 51-57, remove the `confirmedSpending` subtraction if Task 2 confirms `adjustBalance` works correctly. The formula should become: `availableToSpend = (totalBalance - pendingReservations - overdueReservations - creditCardReserved).coerceAtLeast(0L)`. If `adjustBalance` does NOT work, keep the subtraction but also investigate why the balance update isn't propagating. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 4: Update Transaction shadow spec Confirm Transaction Flow** — In `specs/shadow/Transaction.shadow.md` lines 133-137, update the Confirm Transaction Flow to explicitly include `AccountRepository.adjustBalance()`. New flow: `User Confirm |> UpdateTransactionStatusUseCase.execute(id) |> AccountRepository.adjustBalance() |> RecalculateSafeToSpend |> Return Updated Transaction`. (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 5: Run tests and verify fix** — Run the test from Task 1 and all existing balance-related tests (`confirmExpense_deductsAccountBalance`, `confirmIncome_increasesAccountBalance`, `confirmTransfer_decreasesSourceAndIncreasesDestination`). All must pass. Commit with message: `fix: ensure confirmed transactions adjust account balance correctly` (Use Skill: kmp)
