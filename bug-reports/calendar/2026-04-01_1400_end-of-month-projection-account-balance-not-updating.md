# Bug: End of Month Projection Doesn't Reflect Confirmed Transactions; Account Balance Not Updated

**Date:** 2026-04-01
**Feature:** Calendar / End of Month Projection
**Reported symptom:** Safe to spend on home page reflects transactions correctly, but end of month projection and account balances don't get affected when transactions are confirmed

---

## Description

When a user adds and confirms a transaction, the **Safe to Spend** on the home page correctly reflects the change, but the **End of Month Projection** on the calendar and the **account balance** shown in the Accounts list do not update.

**Steps to reproduce:**
1. Create a spending pool account with $500 initial balance
2. Add a $75 EXPENSE transaction (status: PENDING) for a date in the current month
3. Observe: Safe to Spend shows $425 (correct - PENDING deducted)
4. Observe: End of Month Projection shows $425 (correct - pending deducted from pool)
5. Confirm the expense transaction
6. Observe: Safe to Spend remains at $425 (correct - balance already deducted)
7. **BUG:** End of Month Projection shows $500 (WRONG - should remain $425 since balance was already deducted)
8. **BUG:** Account card in Accounts list still shows $500 (WRONG - should show $425)

---

## Root Cause Analysis

### Flow Analysis

The bug involves three interconnected calculations:

**1. Safe to Spend (STS) - `CalculateSafeToSpendUseCase`**
```
availableToSpend = totalBalance 
                 - pendingReservations 
                 - overdueReservations 
                 - creditCardReserved 
                 - confirmedSpending
```

**2. End of Month Projection - `CalculateMonthProjectionUseCase`**
```
poolBalance = sum of spending pool account balances (via getSpendingPoolAccounts)
signedTransactionSum = sum of PENDING/OVERDUE transactions (income +, expense -)

projection = poolBalance + signedTransactionSum
```

**3. Account Balance - Updated via `adjustBalance()` in `UpdateTransactionStatusUseCase`**

### The Problem

The End of Month Projection formula only considers **PENDING/OVERDUE** transactions, not **CONFIRMED** ones. When a transaction is confirmed:

1. `UpdateTransactionStatusUseCase` calls `adjustBalance(accountId, -amount)` for EXPENSE
2. This should update `accounts.balance` column
3. `getSpendingPoolAccounts()` should emit updated accounts with new balances
4. `CalculateMonthProjectionUseCase` should recalculate with updated `poolBalance`

**The issue is that `adjustBalance()` is called but the balance update doesn't propagate to `getSpendingPoolAccounts()` Flow, OR `adjustBalance()` isn't being called at all.**

### Code Inspection

**`UpdateTransactionStatusUseCase.kt` (lines 31-47):**
```kotlin
// Adjust account balance on CONFIRMED transition
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

**`AccountRepositoryImpl.kt` (line 116-118):**
```kotlin
override suspend fun adjustBalance(accountId: Long, delta: Long) {
    database.accountsQueries.adjustAccountBalance(delta, DateUtils.nowMillis(), accountId)
}
```

**`accounts.sq` (lines 40-44):**
```sql
adjustAccountBalance:
UPDATE accounts 
SET balance = balance + :delta, 
    updated_at = :updated_at 
WHERE id = :id;
```

### Potential Root Causes

1. **Flow Not Re-emitting:** The SQLDelight `asFlow()` on `getSpendingPoolAccounts()` may not re-emit after an UPDATE statement, even though it should by default.

2. **Combine Block Issue:** In `CalculateMonthProjectionUseCase`, `getSpendingPoolAccounts()` is called directly in the `combine()` block (line 31), which captures its value once rather than maintaining a reactive subscription.

3. **`adjustBalance` Not Called:** The `adjustBalance()` method might not be invoked because the `CONFIRMED` check passes but the `when` statement doesn't execute properly.

---

## Affected Files

| File | Lines | Issue |
|------|-------|-------|
| `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/CalculateMonthProjectionUseCase.kt` | 30-54 | Uses `getSpendingPoolAccounts()` directly in `combine()` - captures value once |
| `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt` | 31-47 | Calls `adjustBalance()` on CONFIRM but may not be propagating correctly |
| `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt` | 116-118 | `adjustBalance()` implementation |
| `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq` | 40-44 | `adjustAccountBalance` query |
| `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarViewModel.kt` | 80-93 | `monthProjectionJob` collects `CalculateMonthProjectionUseCase` |

---

## Proposed Solution

**Strategy: Verify and fix balance adjustment propagation**

### Step 1: Verify `adjustBalance()` is called
Add logging or breakpoints in `UpdateTransactionStatusUseCase` to confirm `adjustBalance()` is invoked when confirming.

### Step 2: Verify Flow emission after balance update
Check if `getSpendingPoolAccounts().asFlow()` properly re-emits after `adjustAccountBalance` UPDATE statement. If not, consider using a `Query` that watches specific accounts or adding a manual emission trigger.

### Step 3: Fix `CalculateMonthProjectionUseCase` combine block
The current implementation calls `accountRepository.getSpendingPoolAccounts()` directly in `combine()`, which captures the Flow's value once at combination time. This may need to be restructured to properly subscribe to the Flow.

**Option A:** Convert to proper Flow subscription:
```kotlin
return accountRepository.getSpendingPoolAccounts()
    .combine(transactionRepository.getMonthProjectionTransactions(...)) { accounts, transactions ->
        // calculation
    }
```

**Option B:** Use `flatMapLatest` to re-trigger when accounts change:
```kotlin
return accountRepository.getSpendingPoolAccounts()
    .flatMapLatest { accounts ->
        transactionRepository.getMonthProjectionTransactions(...)
            .map { transactions -> Pair(accounts, transactions) }
    }
    .map { (accounts, transactions) -> 
        // calculation
    }
```

### Step 4: Consider adding confirmed transactions to projection
Per the Calendar shadow spec (line 122-128), the projection formula only includes PENDING/OVERDUE. If confirmed transactions should also affect the projection (e.g., to show "confirmed balance + pending commitments"), the formula would need to include `confirmedSpending` similar to how STS does.

---

## Related Bug Reports

- `bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md` - Related STS formula issue
- `bug-reports/safe-to-spend/2026-04-01_1200_account-balance-not-reflecting-transactions.md` - Account balance not updating
- `bug-reports/safe-to-spend/2026-04-01_1335_end-of-month-projection-calculation-issues.md` - Previous EOM projection investigation
