# Bug: Flow Observation Not Triggering — STS and Account Balances Don't Update

**Date:** 2026-04-01
**Feature:** Account Management + Transaction Management + Safe to Spend
**Status:** ROOT CAUSE CONFIRMED

---

## Description

When a user adds an account, STS shows the correct balance. When they add a transaction (PENDING or CONFIRMED), **nothing updates** — neither the Safe to Spend value on the home page nor the account balance on the accounts page.

**Expected Flow (working):**
1. Add account "Savings" with $500 → STS shows $500 ✓
2. Add PENDING expense $75 → STS shows $425 (pending deducted)
3. Confirm expense → Account balance = $425, STS = $425

**Actual Flow (broken):**
1. Add account "Savings" with $500 → STS shows $500 ✓
2. Add PENDING expense $75 → STS still shows $500 ✗
3. Confirm expense → Nothing changes ✗

---

## Root Cause Analysis

### Confirmed: SQLDelight Drivers Don't Auto-Notify

The standard `AndroidSqliteDriver` and `NativeSqliteDriver` **do not** automatically notify query observers when underlying table data changes. SQLDelight's `asFlow()` creates a Flow from queries, but without a notification mechanism, the Flow only ever emits once (on subscription).

### Evidence

**Generated BudgetQueries.kt:**
```kotlin
public fun getTotalSpendingPoolBalance(): Query<Long> = Query(
  -536_635_097, 
  arrayOf("accounts"),  // ← Table dependency tracking
  driver, 
  "budget.sq", 
  "getTotalSpendingPoolBalance", 
  "SELECT CAST...",
  { cursor -> cursor.getLong(0)!! }
)
```

The `Query` object tracks which tables it depends on (`arrayOf("accounts")`), but the driver has no mechanism to notify when those tables change.

### Problem 1: BudgetRepositoryImpl Has No Trigger

```kotlin
// BudgetRepositoryImpl.kt:21-27
override fun getTotalSpendingPoolBalance(): Flow<Long> {
    return database.budgetQueries
        .getTotalSpendingPoolBalance()
        .asFlow()              // ← Just asFlow() - no trigger mechanism
        .mapToOne(dispatcher)
        .map(budgetMapper::toAmount)
}
```

**No trigger exists.** After a mutation (create/update/delete transaction), there's nothing to tell the Flow to re-query.

### Problem 2: AccountRepositoryImpl Has Unused Trigger

```kotlin
// AccountRepositoryImpl.kt:29-30
private val _balanceChangedTrigger = MutableSharedFlow<Unit>(replay = 1)
override val balanceChangedTrigger: SharedFlow<Unit> = _balanceChangedTrigger.asSharedFlow()

// Lines 69, 99, 106, 127 - trigger IS emitted after mutations:
_balanceChangedTrigger.emit(Unit)

// BUT getAllAccounts() never uses it:
override fun getAllAccounts(): Flow<List<Account>> {
    return database.accountsQueries.getAllAccounts(::toEntity)
        .asFlow()              // ← Never combines with balanceChangedTrigger!
        .mapToList(dispatcher)
        .map { entities -> entities.map(accountMapper::toDomain) }
}
```

The `_balanceChangedTrigger` exists and is emitted, but **no one ever collects it**.

### Why This Wasn't Caught Sooner

The existing tests use **FakeAccountRepository** and **FakeTransactionRepository** which manually emit from Flows after mutations. They never test real SQLDelight behavior.

---

## Affected Files

| File | Lines | Issue |
|------|-------|-------|
| `features/budget/data/repository/BudgetRepositoryImpl.kt` | 21-67 | No trigger mechanism - Flows never re-emit |
| `features/accounts/data/repository/AccountRepositoryImpl.kt` | 29-38, 47-53 | `balanceChangedTrigger` emitted but never collected |
| `features/transactions/data/repository/TransactionRepositoryImpl.kt` | 121-142 | No trigger to notify other repositories |

---

## Proposed Solution

### Step 1: Connect balanceChangedTrigger to Account Flows

Modify `AccountRepositoryImpl.getAllAccounts()` and `getSpendingPoolAccounts()` to combine with `balanceChangedTrigger`:

```kotlin
override fun getAllAccounts(): Flow<List<Account>> {
    return database.accountsQueries
        .getAllAccounts(::toEntity)
        .asFlow()
        .mapToList(dispatcher)
        .map { entities -> entities.map(accountMapper.toDomain) }
        .onStart { emit(Unit) }  // Emit immediately on subscription
        .combine(balanceChangedTrigger) { accounts, _ -> accounts }  // Re-emit after changes
}
```

### Step 2: Add Transaction Trigger to Budget Flows

Add a `transactionChangedTrigger` to `TransactionRepositoryImpl` and emit it after `createTransaction`, `updateTransactionStatus`, and `deleteTransaction`. Then combine this with `BudgetRepositoryImpl` flows:

```kotlin
// In BudgetRepositoryImpl
private val _transactionChangedTrigger = MutableSharedFlow<Unit>(replay = 1)
.flowOn(dispatcher)
.shareIn(scope, SharingStarted.Eagerly, 1)

// Combine with existing flows:
override fun getPendingReservations(): Flow<Long> {
    return database.budgetQueries
        .getPendingAndOverdueForSpendingPool(...)
        .asFlow()
        ...
        .combine(transactionChangedTrigger) { rows, _ -> rows }
}
```

### Step 3: Ensure Cross-Repository Triggers

When `UpdateTransactionStatusUseCase` calls `accountRepository.adjustBalance()`, the `balanceChangedTrigger` should cause STS to re-calculate. Ensure `CalculateSafeToSpendUseCase` uses flows that are combined with the trigger.

---

## Verification

1. Add account with $500 → STS shows $500 ✓
2. Add PENDING expense → STS should re-query and show $425
3. If STS doesn't update → Flow trigger not working

---

## Related Bugs

- `bug-reports/account-management/2026-04-01_1515_confirmed-expense-not-deducting-from-account-balance.md`
- `bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md`
- `bug-reports/safe-to-spend/2026-04-01_1200_account-balance-not-reflecting-transactions.md`
