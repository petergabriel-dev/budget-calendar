# Bug (ESCALATED): Transaction Not Affecting Account Balance At All

**Date:** 2026-04-01
**Feature:** Account Management + Transaction Management + Safe to Spend
**Reported symptom:** Transaction doesn't affect the account at all (escalated from confirmed transactions not deducting)

---

## Description

The bug has escalated. Previously, the issue was that confirming a PENDING expense wouldn't deduct from account balance. Now, **transactions don't affect the account at all** — meaning even PENDING expenses aren't being reflected in Safe to Spend calculations.

**Steps to reproduce:**
1. Create a spending pool account with $500 initial balance
2. Add an EXPENSE transaction (status: PENDING) for $75
3. Expected: Safe to Spend should show $425 (STS deducts PENDING expenses)
4. Actual: Safe to Spend still shows $500 — **no deduction occurs**

---

## Root Cause Analysis

### Investigation Findings

After thorough code review, the implementation appears structurally correct:

#### 1. Transaction Creation (CreateTransactionUseCase.kt:39-43)
- Correctly creates PENDING transactions by default
- Does NOT adjust account balance for PENDING (correct per ACC-004)

#### 2. STS Formula (CalculateSafeToSpendUseCase.kt:51-57)
```kotlin
val availableToSpend = (
    budgetData.totalBalance -
        budgetData.pendingReservations -
        budgetData.overdueReservations -
        budgetData.creditCardReserved -
        confirmedSpending
    ).coerceAtLeast(0L)
```
- Formula correctly deducts `pendingReservations` from STS

#### 3. Pending Reservations Query (budget.sq:6-14)
```sql
getPendingAndOverdueForSpendingPool:
SELECT t.status, CAST(t.amount AS INTEGER) AS amount
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.is_in_spending_pool = 1
  AND t.status IN ('pending', 'overdue')
  AND t.type = 'expense'
  AND t.is_sandbox = 0
```
- Query correctly filters for PENDING expenses on spending pool accounts

### Potential Root Cause: Flow Observation Not Triggering

The most likely cause is that **SQLDelight Flow queries are not re-emitting when transactions table changes**.

#### How Flows Should Work:
1. `TransactionRepositoryImpl.createTransaction()` inserts into `transactions` table
2. SQLDelight's `asFlow()` on `getPendingAndOverdueForSpendingPool` should automatically detect the change
3. `BudgetRepositoryImpl.getPendingReservations()` should re-query and emit new value
4. `CalculateSafeToSpendUseCase` should combine and emit new STS

#### Why Flows Might Not Work:
1. **SQLDelight Driver Issue**: The database driver may not be properly observing table changes for Flow re-emission
2. **Missing Database Triggers**: SQLite needs proper triggers or the driver needs to poll for changes
3. **Transaction Isolation**: The insert may be happening in a transaction that the Flow's observation misses

### Key Evidence

The existing test `BudgetUseCasesTest` uses **FakeBudgetRepository** with manual `setTotalSpendingPoolBalance()` and `setPendingReservations()` — it does NOT test real Flow observation from SQLDelight.

The test at `TransactionUseCasesTest.kt:331-376` (`confirmExpense_deductsAccountBalance`) uses **FakeAccountRepository** and **FakeTransactionRepository**, also not testing real database Flow behavior.

This suggests the **integration between SQLDelight Flows and actual database mutations has never been tested**.

---

## Affected Files

| File | Lines | Issue |
|------|-------|-------|
| `features/transactions/data/repository/TransactionRepositoryImpl.kt` | 121-142 | Inserts transaction but no explicit Flow trigger |
| `features/budget/data/repository/BudgetRepositoryImpl.kt` | 29-39 | Depends on Flow re-emission from SQLDelight |
| `core/database/budget.sq` | 6-14 | Query relies on SQLDelight Flow observation |
| `core/database/Database.sq` | 14-28 | transactions table may lack change detection triggers |
| `src/mobile/.../BudgetUseCasesTest.kt` | 260-360 | Tests use fake repositories, not real Flow |
| `src/mobile/.../TransactionUseCasesTest.kt` | 320-376 | Tests use fake repositories, not real Flow |

---

## Proposed Solution

**Strategy: Fix Flow propagation so database changes trigger STS recalculation**

### Step 1: Verify SQLDelight Flow Behavior

Add explicit verification that SQLDelight Flows re-emit on data changes:
- Create a test that uses real `TransactionRepositoryImpl` and `BudgetRepositoryImpl`
- Insert a transaction and verify `getPendingReservations()` Flow re-emits
- If it doesn't re-emit, the issue is in the SQLDelight driver/observation mechanism

### Step 2: Add Manual Flow Triggers (If Needed)

If SQLDelight's automatic observation doesn't work, add manual triggers:
- Option A: Use `MutableSharedFlow` in repositories that is manually emitted after mutations
- Option B: Switch from `asFlow()` to custom observation with database listeners

### Step 3: Ensure Balance Changed Trigger is Collected

The `AccountRepositoryImpl` has `balanceChangedTrigger` (line 30, 127) but it's **never collected**. Consider connecting this to explicitly refresh STS calculations after balance changes.

### Step 4: Integration Tests

Add integration tests that use real database (not fakes) to verify:
- Creating PENDING expense → STS decreases
- Confirming expense → account balance decreases AND STS stays stable
- Cancelling expense → STS restores

---

## Related Bugs

This bug may be related to or exacerbated by:
- `bug-reports/account-management/2026-04-01_1515_confirmed-expense-not-deducting-from-account-balance.md`
- `bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md`
- `bug-reports/safe-to-spend/2026-04-01_1200_account-balance-not-reflecting-transactions.md`
