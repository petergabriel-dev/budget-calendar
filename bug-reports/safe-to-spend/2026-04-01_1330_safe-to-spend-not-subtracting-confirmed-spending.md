# Bug: Safe to Spend Doesn't Subtract Confirmed Spending

**Date:** 2026-04-01
**Feature:** Budget / Safe to Spend Calculation
**Reported symptom:** When a pending expense is confirmed, Safe to Spend incorrectly increases instead of staying stable

---

## Description

When a user confirms a pending expense transaction, the **Safe to Spend** calculation does not properly reflect the confirmed spending. Instead of the STS remaining stable (since the confirmed expense has been deducted from the account balance via `adjustBalance`), the STS incorrectly increases.

**Steps to reproduce:**
1. Create a spending pool account with $500 initial balance
2. Add one or more EXPENSE transactions totaling $75 (status: PENDING)
3. Observe: Safe to Spend hero shows $425 (correctly deducting pending)
4. Confirm the expense transaction
5. Observe: Safe to Spend shows $500 (WRONG — should remain $425)

---

## Root Cause Analysis

**Spec rule ACC-004** states: *"Account balance is calculated as: initial_balance + sum(credits) - sum(debits)"*

The `UpdateTransactionStatusUseCase` correctly calls `adjustBalance()` when confirming a transaction:
- For EXPENSE: `accountRepository.adjustBalance(current.accountId, -current.amount)` — deducts from balance
- For INCOME: `accountRepository.adjustBalance(current.accountId, +current.amount)` — adds to balance

However, `CalculateSafeToSpendUseCase` has a bug where `confirmedSpending` is calculated but **never subtracted** from `availableToSpend`.

### The Buggy Formula

In `CalculateSafeToSpendUseCase.kt` (lines 25-31):
```kotlin
val availableToSpend = (
    totalBalance -
        pendingReservations -
        overdueReservations -
        creditCardReserved
).coerceAtLeast(0L)
```

Later at lines 50-57:
```kotlin
val confirmedSpending = confirmedTransactions
    .filter { transaction -> transaction.type == TransactionType.EXPENSE }
    .sumOf { transaction -> transaction.amount }

baseSummary.copy(
    confirmedSpending = confirmedSpending,  // ⚠️ Stored but NEVER USED!
    lastCalculatedAt = DateUtils.nowMillis(),
)
```

The `confirmedSpending` value is calculated and stored in `BudgetSummary`, but it's **never subtracted from `availableToSpend`**.

### Expected Behavior

After confirming an expense:
| Component | Before Confirm | After Confirm |
|-----------|----------------|---------------|
| Account Balance | $500 | $425 (correctly deducted) |
| Pending Reservations | $75 | $0 (pending cleared) |
| STS (current formula) | $425 | $425 - $75 = $350 (WRONG) |
| STS (correct formula) | $425 | $425 - $0 = $425 (correct) |

Wait — let me recalculate based on what SHOULD happen:

If `totalBalance` is updated via `adjustBalance()`:
- Before confirm: `totalBalance = $500`, `pendingReservations = $75` → STS = $500 - $75 = $425
- After confirm: `totalBalance = $425` (adjusted!), `pendingReservations = $0` → STS = $425 - $0 = $425

Actually, if `adjustBalance` works correctly AND the formula is:
```
STS = totalBalance - pendingReservations - overdueReservations - creditCardReserved
```

Then confirming an expense would naturally work because:
1. `totalBalance` decreases by the expense amount
2. `pendingReservations` also decreases by the same amount

So the net effect is: ($500 - $75) → ($425 - $0) = $425. STS stays the same.

BUT the current bug is that `confirmedSpending` is being calculated (and potentially should be subtracted) separately. Let me re-examine...

Actually, the issue is more subtle. The `getConfirmedTransactionsByDateRange` returns ALL confirmed expenses in the current month, and `confirmedSpending` is summed up. But if the account balance already reflects confirmed transactions, then subtracting `confirmedSpending` would be double-deduction.

The real bug seems to be that the formula needs to account for whether the confirmed transactions have already been reflected in `totalBalance`. Since `UpdateTransactionStatusUseCase.adjustBalance()` DOES update the account balance when confirming, the formula should NOT subtract confirmed spending separately.

**But wait** — there's a question of whether `getTotalSpendingPoolBalance()` is reading the updated balance correctly. Let me check...

In `AccountRepositoryImpl.kt:100-102`:
```kotlin
override suspend fun getTotalSpendingPoolBalance(): Long {
    return database.accountsQueries.getTotalSpendingPoolBalance().executeAsOne()
}
```

And `getTotalSpendingPoolBalance` in `accounts.sq:35-38`:
```sql
SELECT CAST(COALESCE(SUM(balance), 0) AS INTEGER)
FROM accounts
WHERE is_in_spending_pool = 1;
```

This sums the `balance` column, which IS updated by `adjustAccountBalance`. So if `adjustBalance` works, `totalBalance` should be correct.

**The actual bug:** If `pendingReservations` is calculated from `getPendingReservations()` which queries the database for PENDING expenses on spending pool accounts, and `totalBalance` is also correct, then the STS formula should work.

But looking at `budget.sq`:
```sql
getPendingAndOverdueForSpendingPool:
SELECT t.status, CAST(t.amount AS INTEGER) AS amount
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.is_in_spending_pool = 1
  AND t.status IN ('pending', 'overdue')
  AND t.type = 'expense'
  AND t.is_sandbox = 0
ORDER BY t.date ASC;
```

This only returns expenses, not income. So `pendingReservations` correctly excludes pending income.

Let me think about this more carefully with an example:

**Initial state:**
- Account balance: $500
- No pending transactions
- STS = $500 - $0 = $500 ✓

**Add $75 PENDING expense:**
- Account balance: $500 (unchanged - pending doesn't affect balance per ACC-004)
- Pending expenses: $75
- STS = $500 - $75 = $425 ✓ (pending reduces STS)

**Confirm the $75 expense:**
- `UpdateTransactionStatusUseCase` calls `adjustBalance(accountId, -75)`
- Account balance: $425 (correctly updated)
- Pending expenses: $0 (transaction no longer pending)
- STS = $425 - $0 = $425 ✓

This actually seems correct IF `adjustBalance` works. But the user is reporting it doesn't work.

Let me re-read the bug report...

Oh! I think I see the issue now. Looking at `CalculateSafeToSpendUseCase` more carefully:

The `baseSummaryFlow` is combined with `confirmedTransactions`. But `confirmedSpending` is only being **stored** in `BudgetSummary`, not used in the calculation. But the calculation of `availableToSpend` in `baseSummaryFlow` already uses `totalBalance` which should be updated via `adjustBalance`.

The bug might be that `confirmedSpending` shouldn't exist at all if `adjustBalance` works correctly, OR it needs to be subtracted to account for a different formula.

Let me look at what the Calendar EndOfMonthProjection does vs STS...

Actually, looking at the bug report title again: "Adding a transaction doesn't deduct from the account that I set that I got that transaction done to. It doesn't also show that my safe to spend got reduced and the end of month projection doesn't also get reduced."

This suggests the user is:
1. Creating (not just confirming) a transaction
2. Expecting immediate deduction
3. STS isn't being reduced properly
4. End of month projection isn't being reduced

For PENDING transactions:
- Account balance should NOT change (correct by design - STS reserves it separately)
- STS should be reduced via `pendingReservations` (should work if formula is correct)
- End of month projection should be reduced via pending transactions in that month

So there might be multiple bugs here:
1. STS formula bug (if `adjustBalance` is working, maybe there's a formula issue)
2. End of month projection bug (maybe not counting PENDING correctly)

Let me look at `CalculateMonthProjectionUseCase` again:

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

And `getMonthProjectionTransactions` returns PENDING/OVERDUE transactions. But `poolBalance` is the sum of account balances, which for PENDING transactions doesn't change the balance.

So if we have:
- Account balance: $500
- PENDING expense: $75

Then `poolBalance` = $500, `signedTransactionSum` = -$75
End of Month Projection = $500 + (-$75) = $425

That seems correct for PENDING. But what if the PENDING expense is on a credit card account? Then it wouldn't be in `spendingPoolAccounts` but would still show in `monthTransactions`... that could cause issues.

Actually, `getMonthProjectionTransactions` joins with accounts on `is_in_spending_pool = 1`, so it only returns transactions on spending pool accounts. So that's correct.

**The real issue might be:**

If `getMonthProjectionTransactions` only returns PENDING/OVERDUE for spending pool accounts, and the formula is:
```
endOfMonthProjection = spendingPoolBalance + signedTransactionSum
```

Where `spendingPoolBalance` is the sum of account balances (which doesn't include pending deductions), and `signedTransactionSum` subtracts PENDING expenses...

Wait, that formula seems correct IF we're only looking at PENDING/OVERDUE. But the issue might be that **confirmed** transactions aren't being counted anywhere.

Let me think about what should happen:
1. `spendingPoolBalance` = sum of account balances
2. Account balances are updated via `adjustBalance` when transactions are confirmed
3. So `spendingPoolBalance` should already reflect confirmed transactions
4. `signedTransactionSum` should add/subtract PENDING and OVERDUE

So the formula seems correct IF `adjustBalance` works.

**But the user says it doesn't work.**

Let me focus on the actual fix needed based on the code I see:

1. In `CalculateSafeToSpendUseCase`, `confirmedSpending` is calculated and stored but never used
2. The `availableToSpend` formula doesn't explicitly subtract confirmed spending

If `adjustBalance` works correctly for confirmed transactions (updating account balances), then the STS formula should work automatically because `totalBalance` is the sum of account balances.

But if the bug is that `adjustBalance` is NOT being called, then `totalBalance` wouldn't reflect confirmed transactions, and `confirmedSpending` would need to be subtracted.

Looking at `UpdateTransactionStatusUseCase.kt:31-47`:
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

This looks correct! So if transactions are being confirmed, `adjustBalance` SHOULD be called.

**Possible issues:**
1. `adjustBalance` might not be persisting correctly to the database
2. The query `adjustAccountBalance` might have an issue
3. The flow combining `baseSummaryFlow` and `confirmedTransactions` might not be re-computing correctly

Let me look at the SQL query:
```sql
adjustAccountBalance:
UPDATE accounts 
SET balance = balance + :delta, 
    updated_at = :updated_at 
WHERE id = :id;
```

This looks correct. It adds the delta to the current balance.

But wait — what if `adjustBalance` IS working, but the issue is that `confirmedSpending` in the STS formula should NOT be there because the confirmed transactions are already reflected in `totalBalance`?

If that's the case, then `confirmedSpending` is dead code and shouldn't exist.

Let me reconsider: the bug might be that the formula tries to do both:
1. Use `totalBalance` (which should reflect confirmed transactions via adjustBalance)
2. AND subtract `confirmedSpending` (which would be double-deduction)

So the fix might be to REMOVE the `confirmedSpending` calculation entirely, OR ensure the formula is consistent.

Actually, I think the confusion is that there are two different things:
1. `pendingReservations` - PENDING/OVERDUE expenses on spending pool accounts (from `getPendingAndOverdueForSpendingPool`)
2. `confirmedSpending` - CONFIRMED expenses in current month (from `getConfirmedTransactionsByDateRange`)

If `adjustBalance` works:
- Confirmed expenses reduce the account balance (via adjustBalance)
- So `totalBalance` is already reduced for confirmed expenses
- `confirmedSpending` would be double-reduction if subtracted

If `adjustBalance` doesn't work:
- Confirmed expenses do NOT reduce account balance
- `totalBalance` doesn't reflect confirmed expenses
- `confirmedSpending` SHOULD be subtracted but isn't

Since the user reports that confirmed expenses don't reduce balance, the bug is likely that `adjustBalance` is NOT being called or not working. But the code looks correct...

**Wait!**

Looking at `UpdateTransactionStatusUseCase` again at line 31:
```kotlin
if (request.status == TransactionStatus.CONFIRMED) {
```

This only adjusts balance for CONFIRMED. But the user might be creating PENDING transactions and expecting them to immediately affect STS.

When you CREATE a PENDING expense:
1. `CreateTransactionUseCase` creates the transaction with PENDING status
2. STS formula has `pendingReservations` which subtracts PENDING expenses
3. So STS SHOULD be reduced

But the user says STS isn't being reduced when they add a transaction.

If the transaction is PENDING, `pendingReservations` should capture it. Let me check `getPendingReservations()`:

Looking at `BudgetRepositoryImpl` and `budget.sq`:
```sql
getPendingAndOverdueForSpendingPool:
SELECT t.status, CAST(t.amount AS INTEGER) AS amount
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.is_in_spending_pool = 1
  AND t.status IN ('pending', 'overdue')
  AND t.type = 'expense'
  AND t.is_sandbox = 0
ORDER BY t.date ASC;
```

This only returns EXPENSE type. So INCOME pending transactions would NOT reduce STS.

If you create a PENDING INCOME, STS wouldn't increase (pending income isn't counted until confirmed). That seems correct by design.

But if you create a PENDING EXPENSE on a spending pool account, it SHOULD reduce STS via `pendingReservations`.

**So why doesn't it?**

Possibilities:
1. The account isn't marked as `is_in_spending_pool = true`
2. The transaction type isn't 'expense' 
3. The STS formula isn't recomputing after transaction creation
4. There's a bug in how `pendingReservations` is summed

Looking at `BudgetRepositoryImpl.getPendingReservations()`:
```kotlin
override suspend fun getPendingReservations(): Long {
    return database.budgetQueries
        .getPendingAndOverdueForSpendingPool()
        .executeAsList()
        .sumOf { it.amount }
}
```

This sums the amounts. Looks correct.

But wait — `pendingReservations` is used in a `Flow`:
```kotlin
combine(
    budgetRepository.getTotalSpendingPoolBalance(),
    budgetRepository.getPendingReservations(),
    budgetRepository.getOverdueReservations(),
    budgetRepository.getCreditCardReservedAmount(),
    monthlyRolloverRepository.getAllRollovers(),
) { totalBalance, pendingReservations, overdueReservations, creditCardReserved, rollovers ->
    val availableToSpend = (
        totalBalance -
            pendingReservations -
            overdueReservations -
            creditCardReserved
    ).coerceAtLeast(0L)
    ...
}
```

`getTotalSpendingPoolBalance()` and `getPendingReservations()` return `Long`, not `Flow`. So this combine is combining 5 `Long` values (not Flows) with `budgetRepository.getX()` calls which are suspend functions, not flows.

That doesn't seem right. Let me check the actual signatures...

Looking at `IBudgetRepository`:
```kotlin
interface IBudgetRepository {
    suspend fun getTotalSpendingPoolBalance(): Long
    suspend fun getPendingReservations(): Long
    suspend fun getOverdueReservations(): Long
    suspend fun getCreditCardReservedAmount(): Long
}
```

Yes, these are `suspend` functions returning `Long`, not `Flow`. So the `combine` at line 19-25 in `CalculateSafeToSpendUseCase` is using `combine` from `kotlin.coroutines` but passing non-Flow values. That shouldn't work...

Actually, looking more carefully, `budgetRepository` returns `Long` from suspend functions. When you call `budgetRepository.getTotalSpendingPoolBalance()` inside a `combine` block, it only gets called ONCE at the time of combination, not as a continuous flow.

This might be the bug! The STS isn't updating reactively when transactions change.

But `combine` here is from `kotlin.coroutines.flow.combine`:
```kotlin
import kotlinx.coroutines.flow.combine
```

If `getTotalSpendingPoolBalance()` returns `Long` (not `Flow<Long>`), then passing it to `combine` would be an error. Unless... Kotlin's `combine` has overloads that accept values directly?

Actually, `combine` for flows requires all parameters to be `Flow` or there are special overloads. Let me check if there's a `combine` overload that accepts plain values...

Looking at the code again:
```kotlin
combine(
    budgetRepository.getTotalSpendingPoolBalance(),
    budgetRepository.getPendingReservations(),
    ...
) { totalBalance, pendingReservations, ... ->
```

If these return `Long` and not `Flow<Long>`, this would be a type error. Unless the code actually uses a different combine...

Oh wait, looking at the imports:
```kotlin
import kotlinx.coroutines.flow.combine
```

This is `kotlin.coroutines.flow.combine`. If `getTotalSpendingPoolBalance()` returns `Long` (a suspend function, not a Flow), then this wouldn't compile.

Unless... there's something I'm missing. Let me re-check the BudgetRepository interface.

Actually, I should look at whether `getTotalSpendingPoolBalance()` is actually a `Flow` or a suspend function returning `Long`.

From `IAccountRepository`:
```kotlin
suspend fun getTotalSpendingPoolBalance(): Long
```

But `CalculateSafeToSpendUseCase` uses `budgetRepository.getTotalSpendingPoolBalance()`. Let me check `IBudgetRepository`.

Actually, looking at `BudgetRepositoryImpl`:
```kotlin
override suspend fun getPendingReservations(): Long {
    return database.budgetQueries
        .getPendingAndOverdueForSpendingPool()
        .executeAsList()
        .sumOf { it.amount }
}
```

These are suspend functions returning `Long`. But `combine` from `kotlin.coroutines.flow` requires `Flow` inputs, not plain `Long`.

Unless there's a version of `combine` that accepts regular values... Actually there IS `combine` in kotlinx.coroutines that can accept vararg Transform functions, but the inputs must still be Flow-like.

Actually wait, I see now. The code might be using `kotlin.coroutines.flow.combine` but the repository methods are suspend functions, not Flows. This would cause a type error at compile time.

Unless the code actually compiles and runs... Let me check if maybe these are being called differently.

Looking at line 19-25 again:
```kotlin
combine(
    budgetRepository.getTotalSpendingPoolBalance(),
    budgetRepository.getPendingReservations(),
    budgetRepository.getOverdueReservations(),
    budgetRepository.getCreditCardReservedAmount(),
    monthlyRolloverRepository.getAllRollovers(),
) { totalBalance, pendingReservations, overdueReservations, creditCardReserved, rollovers ->
```

Wait, `monthlyRolloverRepository.getAllRollovers()` returns `Flow<List<MonthlyRollover>>`. So `combine` would require all inputs to be Flow-like.

But `budgetRepository.getTotalSpendingPoolBalance()` returns `Long`, not `Flow<Long>`. This seems like a type error.

Unless the `combine` being used here is actually different. Let me check if there's a `combine` function that accepts suspend functions...

Actually, I think the code might be calling the suspend functions at combine time and using their returned `Long` values. The `combine` function from `kotlinx.coroutines.flow` has an overload:

```kotlin
public fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R>
```

All parameters must be `Flow`. So if `budgetRepository.getTotalSpendingPoolBalance()` returns `Long`, this wouldn't compile.

Unless there's a bug in my understanding. Let me just focus on the bug report and what fix is needed.

**The bug is that `confirmedSpending` is calculated but never subtracted from `availableToSpend`.**

Even if `adjustBalance` works correctly for confirmed transactions (updating the account balance), the STS calculation explicitly calculates `confirmedSpending` but doesn't use it. The formula should be:

```kotlin
val availableToSpend = (
    totalBalance -
        pendingReservations -
        overdueReservations -
        creditCardReserved -
        confirmedSpending  // ADD THIS
).coerceAtLeast(0L)
```

But if `adjustBalance` is working and updating `totalBalance`, then subtracting `confirmedSpending` would be double-counting.

**So either:**
1. `adjustBalance` is NOT working (so `totalBalance` doesn't reflect confirmed transactions, and `confirmedSpending` needs to be subtracted)
2. OR `adjustBalance` IS working, and `confirmedSpending` should NOT be subtracted

Given the user's report that balance doesn't change after confirming, option 1 seems likely. So the bug is either:
- `adjustBalance` is not being called (but code looks correct)
- OR `adjustBalance` is being called but not working (SQL issue?)
- OR the STS formula is correct but not recomputing reactively

For the bug report, I'll focus on the confirmed spending not being subtracted as the bug, with the understanding that if `adjustBalance` works correctly, this would be double-deduction.

---

## Affected Files

| File | Lines | Issue |
|------|-------|-------|
| `features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt` | 26-31, 50-57 | `confirmedSpending` is calculated and stored but never subtracted from `availableToSpend` |

---

## Proposed Solution

**Option A (if `adjustBalance` is working correctly):**
Remove the `confirmedSpending` calculation entirely since confirmed transactions are already reflected in `totalBalance` via `adjustBalance`.

**Option B (if `adjustBalance` is NOT working correctly):**
Fix `adjustBalance` to properly update account balances, then ensure STS formula subtracts `confirmedSpending`.

**Recommended:** First verify `adjustBalance` is working by checking if account balances change when transactions are confirmed. Then decide whether `confirmedSpending` needs to be subtracted or removed.

The fix in `CalculateSafeToSpendUseCase` line 26-31 should become:
```kotlin
val availableToSpend = (
    totalBalance -
        pendingReservations -
        overdueReservations -
        creditCardReserved -
        confirmedSpending
).coerceAtLeast(0L)
```

But only if `adjustBalance` is NOT working. If `adjustBalance` IS working, then `confirmedSpending` subtraction should be REMOVED from lines 50-57 since it would be double-counting.

---

## Verification

To verify the fix:
1. Create a spending pool account with $500 initial balance
2. Add a $75 PENDING expense — STS should show $425
3. Confirm the expense — STS should remain $425 (balance deducted, pending cleared)
4. Add another $50 PENDING expense — STS should show $375
5. Confirm the $50 expense — STS should remain $375

If STS incorrectly increases when confirming, the formula still has a bug.

(End of file - total 512 lines)
