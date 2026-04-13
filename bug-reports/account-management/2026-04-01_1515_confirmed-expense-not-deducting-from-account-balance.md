# Bug: Confirmed Expense Transaction Not Deducting from Account Balance

**Date:** 2026-04-01
**Feature:** Account Management + Transaction Management
**Reported symptom:** Confirmed spending transaction is not deducting from the account balance

---

## Description

When a user confirms an expense transaction, the account balance is not being updated to reflect the deduction. The user expects that when they confirm a spending transaction, the account balance decreases by the expense amount, but the balance remains unchanged.

**Steps to reproduce:**
1. Create a spending pool account with an initial balance (e.g., $500)
2. Add an EXPENSE transaction (status: PENDING) for $75
3. Observe: PENDING expenses do not affect the account balance (correct per ACC-004)
4. Confirm the expense transaction
5. Expected: Account balance should decrease to $425
6. Actual: Account balance remains at $500

---

## Root Cause Analysis

### Backend Logic Review

After reviewing the backend logic, the following components were analyzed:

#### 1. UpdateTransactionStatusUseCase (lines 31-47)

The use case correctly calls `adjustBalance()` when a transaction is confirmed:

```kotlin
if (request.status == TransactionStatus.CONFIRMED) {
    when (current.type) {
        TransactionType.EXPENSE -> accountRepository.adjustBalance(current.accountId, -current.amount)
        TransactionType.INCOME -> accountRepository.adjustBalance(current.accountId, current.amount)
        TransactionType.TRANSFER -> {
            accountRepository.adjustBalance(current.accountId, -current.amount)
            current.linkedTransactionId?.let { linkedId ->
                val linkedTx = transactionRepository.getTransactionById(linkedId)
                linkedTx?.let { accountRepository.adjustBalance(it.accountId, current.amount) }
            }
        }
    }
}
```

**Finding:** Logic is correct for EXPENSE type - it passes `-current.amount` to deduct from balance.

#### 2. AccountRepositoryImpl.adjustBalance() (lines 125-128)

```kotlin
override suspend fun adjustBalance(accountId: Long, delta: Long) {
    database.accountsQueries.adjustAccountBalance(delta, DateUtils.nowMillis(), accountId)
    _balanceChangedTrigger.emit(Unit)
}
```

**Finding:** Correctly calls `adjustAccountBalance` SQL and emits balance change trigger.

#### 3. SQL Query adjustAccountBalance (accounts.sq:40-44)

```sql
adjustAccountBalance:
UPDATE accounts 
SET balance = balance + :delta, 
    updated_at = :updated_at 
WHERE id = :id;
```

**Finding:** SQL is correct - for EXPENSE with delta=-75, it executes `SET balance = balance + (-75)`.

#### 4. CalculateSafeToSpendUseCase STS Formula (lines 51-57)

```kotlin
val availableToSpend = (
    budgetData.totalBalance -
        budgetData.pendingReservations -
        budgetData.overdueReservations -
        budgetData.creditCardReserved -
        confirmedSpending
    ).coerceAtLeast(0L)
```

**Finding:** `confirmedSpending` IS being subtracted from STS. The code also has a safeguard comment (lines 45-50) noting potential double-deduction if `adjustBalance` works correctly.

### Potential Issues Identified

#### Issue 1: Double-Deduction Risk in STS Formula

The STS formula has both:
1. `totalBalance` - which should reflect confirmed transactions via `adjustBalance`
2. `confirmedSpending` subtraction - which also subtracts confirmed expenses

**If `adjustBalance` works correctly:**
- `totalBalance` decreases by expense amount
- `confirmedSpending` also subtracts expense amount
- Result: **Double-deduction** of confirmed expenses

**If `adjustBalance` doesn't work:**
- `totalBalance` doesn't reflect confirmed expenses
- `confirmedSpending` subtraction compensates
- Result: STS correctly reflects confirmed expenses

The comment in the code acknowledges this:
```kotlin
// Note: If adjustBalance() works correctly (updates totalBalance when confirming),
// this could lead to double-deduction. The confirmedSpending subtraction is
// a safeguard for the case where adjustBalance hasn't propagated.
```

#### Issue 2: Flow Emission Not Connected

The `balanceChangedTrigger` (line 30, 127 in AccountRepositoryImpl) is emitted after balance changes but is **never collected or used** anywhere in the codebase. The Flow-based queries rely on SQLDelight's internal observation mechanism rather than this trigger.

#### Issue 3: Shadow Spec Gap

The Transaction shadow spec's "Confirm Transaction Flow" (lines 133-137) only specifies:
1. `TransactionRepository.updateStatus(CONFIRMED)`
2. `RecalculateSafeToSpend`

It does **not** explicitly require calling `adjustBalance()` to update the account balance. The current implementation added this step but the spec wasn't updated to reflect it.

---

## Affected Files

| File | Lines | Issue/Notes |
|------|-------|-------------|
| `features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt` | 31-47 | Logic correct, calls `adjustBalance` for CONFIRMED |
| `features/accounts/data/repository/AccountRepositoryImpl.kt` | 125-128 | Correctly implements `adjustBalance` |
| `core/database/accounts.sq` | 40-44 | SQL query correct |
| `features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt` | 38-57 | Has `confirmedSpending` subtraction (potential double-deduction) |
| `specs/shadow/Transaction.shadow.md` | 133-137 | Spec doesn't mention `adjustBalance` requirement |

---

## Proposed Solution

**Strategy: Investigate Flow propagation and clarify STS formula intent**

### Step 1: Verify adjustBalance Propagation

Add logging to verify that `adjustBalance` is actually being called and the SQL is executing:
- Add log statements in `AccountRepositoryImpl.adjustBalance()`
- Verify the SQL UPDATE completes successfully
- Check database directly after confirming a transaction

### Step 2: Fix STS Formula Consistency

The current implementation has a potential double-deduction issue. Two options:

**Option A (Preferred):** If `adjustBalance` works correctly, **remove** the `confirmedSpending` subtraction since `totalBalance` already reflects confirmed transactions. This requires verifying `adjustBalance` propagates correctly to Flows.

**Option B:** If `adjustBalance` has propagation issues, **keep** `confirmedSpending` subtraction as a safeguard but investigate why the Flow-based balance queries aren't updating.

### Step 3: Update Shadow Spec

Update `specs/shadow/Transaction.shadow.md` to explicitly require calling `adjustBalance` in the Confirm Transaction Flow:
```
Confirm Transaction Flow
  |> TransactionRepository.updateStatus(CONFIRMED)
  |> AccountRepository.adjustBalance()  // ADD THIS
  |> RecalculateSafeToSpend
  |> Return Updated Transaction
```

### Step 4: Connect balanceChangedTrigger (Optional)

Consider using the `balanceChangedTrigger` to manually trigger UI refreshes if SQLDelight Flow observation isn't reliable.

---

## Verification Steps

To verify the bug:
1. Create an account with initial balance $500
2. Create a PENDING expense for $75
3. Confirm the expense
4. Query the database directly: `SELECT balance FROM accounts WHERE id = ?`
5. Check if balance is $425 or still $500

If balance is still $500 after confirming, the issue is `adjustBalance` not working or not being called.

If balance is $425 but STS shows wrong value, the issue is in the STS formula.
