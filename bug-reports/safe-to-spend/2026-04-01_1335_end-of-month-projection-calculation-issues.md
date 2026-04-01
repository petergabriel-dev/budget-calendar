# Bug: End of Month Projection Calculation Issues

**Date:** 2026-04-01
**Feature:** Calendar / End of Month Projection
**Reported symptom:** End of month projection doesn't reflect transaction deductions

---

## Description

When a user adds a transaction (either PENDING or CONFIRMED), the end of month projection displayed on the calendar doesn't properly reflect the deduction. The user expects that adding an expense transaction would reduce their end of month projection.

**Steps to reproduce:**
1. Create a spending pool account with $500 initial balance
2. Add a $75 EXPENSE transaction for a date within the current month
3. Observe: End of Month Projection shows $500 (should show $425)
4. Confirm the expense transaction
5. Observe: End of Month Projection still shows $500 (should remain $425 if balance was already deducted, or show different value depending on formula)

---

## Root Cause Analysis

The `CalculateMonthProjectionUseCase` calculates the end of month projection using the following formula:

```kotlin
poolBalance + signedTransactionSum
```

Where:
- `poolBalance` = sum of all spending pool account balances
- `signedTransactionSum` = sum of PENDING/OVERDUE transactions in the month (income added, expenses subtracted)

### Potential Issues

#### Issue 1: Potential Double-Deduction if `adjustBalance` Works

If `UpdateTransactionStatusUseCase.adjustBalance()` is working correctly:
- When an EXPENSE is confirmed, `adjustBalance(accountId, -amount)` is called
- This reduces the account's `balance` field in the database
- `poolBalance` (sum of account balances) would already be reduced

But `signedTransactionSum` from `getMonthProjectionTransactions` only returns PENDING/OVERDUE transactions. So confirmed transactions wouldn't be in `signedTransactionSum`.

If a PENDING expense is later confirmed:
1. `poolBalance` decreases by the expense amount (via `adjustBalance`)
2. The transaction is no longer PENDING, so `signedTransactionSum` no longer subtracts it

So: ($500 pool - $75) + $0 = $425. Correct!

But wait — if `adjustBalance` is NOT working:
1. `poolBalance` stays at $500 (balance not updated)
2. PENDING expense is in `signedTransactionSum` as -$75
3. End of Month Projection = $500 + (-$75) = $425

This also gives $425, which seems correct for PENDING transactions.

**The question is: what about confirmed transactions?**

If an expense was CONFIRMED in a previous month (balance already deducted), it wouldn't appear in `getMonthProjectionTransactions` (only PENDING/OVERDUE). So it wouldn't be double-counted.

But if an expense is CONFIRMED in the current month:
- If `adjustBalance` works: `poolBalance` is reduced, transaction not in `signedTransactionSum` → correct
- If `adjustBalance` doesn't work: `poolBalance` not reduced, transaction not in `signedTransactionSum` → projection too high

#### Issue 2: Formula Might Need to Account for Confirmed Spending

Looking at the Calendar shadow spec constraint:
```
Formula: sum(spending_pool_account_balances)
        + sum(PENDING income transactions where date <= lastDayOfCurrentMonth)
        - sum(PENDING expense transactions where date <= lastDayOfCurrentMonth)
        + sum(OVERDUE income transactions where date <= lastDayOfCurrentMonth)
        - sum(OVERDUE expense transactions where date <= lastDayOfCurrentMonth)
(only non-sandbox, only spending pool accounts)
```

This matches the current implementation. But there's an implicit assumption that account balances already reflect CONFIRMED transactions.

**If `adjustBalance` is NOT being called when confirming transactions**, then confirmed transactions have never been deducted from the account balance, and the projection would be wrong.

#### Issue 3: Query Filters by Spending Pool Account

The `getMonthProjectionTransactions` query joins with accounts:
```sql
getPendingAndOverdueForSpendingPoolInRange:
SELECT t.*
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE t.status IN ('pending', 'overdue')
  AND t.date >= :startMillis
  AND t.date <= :endMillis
  AND a.is_in_spending_pool = 1
  AND t.is_sandbox = 0
ORDER BY t.date ASC;
```

This only returns transactions where `account_id` is a spending pool account. This is correct per the spec.

But what about TRANSFER transactions? The formula in `CalculateMonthProjectionUseCase` returns 0 for TRANSFER:
```kotlin
TransactionType.TRANSFER -> 0L
```

This seems correct because transfers are just movement of money between accounts, not spending.

#### Issue 4: Month Boundaries

The query uses `t.date >= :startMillis AND t.date <= :endMillis`. This correctly filters to the target month.

But what about recurring transactions that span multiple months? That's handled by `RecurringGenerationUtils`, but if a recurring transaction is generated for a future month, it would have a date in that future month, so the current month's projection wouldn't include it. This seems correct.

---

## Comparison: STS vs End of Month Projection

| Aspect | Safe to Spend (STS) | End of Month Projection |
|--------|-------------------|------------------------|
| Formula | `totalBalance - pending - overdue - ccReserved` | `poolBalance + signedTransactionSum` |
| Pending included? | Yes (via `pendingReservations`) | Yes (via `signedTransactionSum`) |
| Overdue included? | Yes (via `overdueReservations`) | Yes (via `signedTransactionSum`) |
| CC Reserved included? | Yes | No (separate concept) |
| `confirmedSpending` considered? | Calculated but not subtracted (bug?) | No (assumes balance already reflects confirmed) |
| `adjustBalance` required? | Yes (to update totalBalance) | Yes (to update poolBalance) |

---

## Affected Files

| File | Lines | Issue |
|------|-------|-------|
| `features/calendar/domain/usecase/CalculateMonthProjectionUseCase.kt` | 30-48 | Formula may be incorrect if `adjustBalance` isn't working |
| `core/database/transactions.sq` | 95-104 | `getPendingAndOverdueForSpendingPoolInRange` only returns PENDING/OVERDUE |

---

## Proposed Solution

### Verify `adjustBalance` is Working

First, check if `UpdateTransactionStatusUseCase.adjustBalance()` is properly updating account balances when transactions are confirmed. If it's not working, fix that first.

### Fix End of Month Projection Formula

The formula should be:

```kotlin
val poolBalance = spendingPoolAccounts.sumOf { account -> account.balance }
val signedTransactionSum = monthTransactions.sumOf { transaction ->
    when (transaction.type) {
        TransactionType.INCOME -> transaction.amount
        TransactionType.EXPENSE -> -transaction.amount
        TransactionType.TRANSFER -> 0L
    }
}
poolBalance + signedTransactionSum
```

This assumes:
1. `poolBalance` (sum of spending pool account balances) reflects all CONFIRMED transactions via `adjustBalance`
2. `signedTransactionSum` adds PENDING/OVERDUE income and subtracts PENDING/OVERDUE expenses

If this formula gives wrong results, check:
1. Is `adjustBalance` being called when transactions are confirmed?
2. Is the account properly marked as `is_in_spending_pool = true`?
3. Is the transaction date within the target month?

### Alternative: Explicit Formula

If `adjustBalance` is unreliable, consider an explicit formula that doesn't depend on account balances:

```kotlin
// Calculate from transactions only (not relying on adjustBalance)
val confirmedBalance = initialBalances + confirmedIncome - confirmedExpenses

val poolBalance = spendingPoolAccounts.sumOf { account -> account.balance }
// This should equal confirmedBalance if adjustBalance works

val pendingProjection = monthTransactions.sumOf { transaction ->
    when (transaction.type) {
        TransactionType.INCOME -> transaction.amount
        TransactionType.EXPENSE -> -transaction.amount
        TransactionType.TRANSFER -> 0L
    }
}

poolBalance + pendingProjection
```

This is what the current code does, and it should be correct IF:
1. `poolBalance` reflects confirmed transactions (via `adjustBalance`)
2. `pendingProjection` only includes PENDING/OVERDUE (not confirmed)

---

## Edge Cases to Consider

### Case 1: Credit Card Expense on CC Account (Not in Spending Pool)

If an expense is on a credit card account (not in spending pool):
- It won't appear in `getMonthProjectionTransactions` (because of the `is_in_spending_pool = 1` join condition)
- The CC reserved amount handles this in STS via `creditCardReserved`
- But end of month projection doesn't subtract CC expenses

Is this correct? Per the spec, end of month projection is "sum of spending pool account balances + pending transactions on spending pool accounts". CC expenses would reduce the user's ability to pay their CC, but wouldn't directly affect the spending pool balance.

However, if the user plans to pay the CC from a spending pool account, that payment would be a TRANSFER from the spending pool to the CC. If that TRANSFER is PENDING, it should probably be counted.

### Case 2: Transfer from Spending Pool to Savings

If a user transfers $100 from Checking (spending pool) to Savings (not spending pool):
- The Checking account (spending pool) would have its balance reduced
- But if the transfer is PENDING, `getMonthProjectionTransactions` returns 0 for TRANSFER
- So the projection wouldn't reflect the pending transfer

This seems like a bug. A PENDING transfer should reduce the projection because the money is leaving the spending pool.

The fix would be to modify the formula for TRANSFER:
```kotlin
TransactionType.TRANSFER -> {
    // For transfers FROM spending pool, subtract the amount
    // For transfers TO spending pool, add the amount
    // But we need to know direction...
}
```

This would require knowing the source/destination and whether they're in the spending pool.

### Case 3: Future Month Transactions

Per the spec: "A bill due in a future month does NOT reduce the current month's projection"

The current query filters by `t.date >= :startMillis AND t.date <= :endMillis`, so only transactions in the target month are included. This is correct.

---

## Verification

To verify the fix:
1. Create a spending pool account with $500 initial balance
2. Add a $75 PENDING expense for a date in the current month
3. End of Month Projection should show $425
4. Confirm the expense
5. End of Month Projection should remain $425 (balance deducted, pending cleared)

If End of Month Projection incorrectly changes when confirming, the formula has a bug.

(End of file - total 315 lines)
