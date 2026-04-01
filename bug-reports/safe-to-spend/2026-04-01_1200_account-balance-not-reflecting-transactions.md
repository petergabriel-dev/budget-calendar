# Bug: Account Balance Does Not Reflect Transactions; Safe to Spend / Account Display Mismatch

**Date:** 2026-04-01
**Feature:** Safe to Spend Calculation + Account Management + Transaction Management
**Reported symptom:** Spending account shows $500 but Safe to Spend shows $425

---

## Description

When a user adds an expense transaction against a spending pool account, the **Safe to Spend** correctly deducts the pending amount (e.g., $500 - $75 pending = $425), but the **account balance** displayed on the Account card remains unchanged at $500. The user sees two contradictory numbers and has no way to reconcile them from the UI.

**Steps to reproduce:**
1. Create a spending pool account with $500 initial balance
2. Add one or more EXPENSE transactions totaling $75 (status: PENDING)
3. Observe: Safe to Spend hero shows $425, Account card still shows $500
4. Confirm the transaction — Safe to Spend jumps back to $500 (incorrect)

---

## Root Cause Analysis

There are **two distinct bugs** in the current implementation:

### Bug 1: Account balance is static — never updated by transactions

**Spec rule ACC-004** states: *"Account balance is calculated as: initial_balance + sum(credits) - sum(debits)"*

The current implementation stores `balance` as a static field in the `accounts` table (Database.sq:5) that is only changed via explicit `updateAccount()` calls. Neither `CreateTransactionUseCase` nor `UpdateTransactionStatusUseCase` ever adjusts the account balance.

- `CreateTransactionUseCase` (lines 39-43) calls `transactionRepository.createTransaction()` but never touches `AccountRepository`
- `UpdateTransactionStatusUseCase` (lines 11-28) updates transaction status but never adjusts account balance on CONFIRMED/CANCELLED transitions
- `AccountCard` (line 85) renders `balance` directly from the static DB field

The account balance displayed to the user never changes after initial creation, regardless of how many transactions are recorded or confirmed.

### Bug 2: Confirming a transaction causes Safe to Spend to incorrectly increase

The Safe to Spend formula in `CalculateSafeToSpendUseCase` (lines 26-31):
```
availableToSpend = totalBalance - pendingReservations - overdueReservations - creditCardReserved
```

- `totalBalance` = static account balance (never changes)
- `pendingReservations` = sum of PENDING expenses
- `overdueReservations` = sum of OVERDUE expenses

When a PENDING expense is CONFIRMED:
- It drops out of `pendingReservations` (no longer pending)
- `totalBalance` stays the same (Bug 1 — balance never updates)
- `confirmedSpending` is calculated (lines 50-52) but **never subtracted** from `availableToSpend`
- Result: Safe to Spend **increases** when money is spent — the opposite of correct behavior

**Example scenario:**
| Event | Account Balance | Pending | STS |
|-------|----------------|---------|-----|
| Initial state | $500 | $0 | $500 |
| Add $75 expense (PENDING) | $500 (wrong per ACC-004) | $75 | $425 (correct) |
| Confirm the $75 expense | $500 (wrong) | $0 | $500 (wrong — should be $425) |

---

## Affected Files

| File | Lines | Issue |
|------|-------|-------|
| `features/transactions/domain/usecase/CreateTransactionUseCase.kt` | 39-43 | Does not update account balance after creating transaction |
| `features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt` | 11-28 | Does not update account balance on CONFIRMED/CANCELLED transitions |
| `features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt` | 26-31, 54 | `confirmedSpending` is computed but not subtracted from `availableToSpend` |
| `features/accounts/presentation/components/AccountCard.kt` | 85 | Displays static `balance` which never reflects transactions |
| `features/accounts/presentation/AccountViewModel.kt` | 56 | Maps balance directly from static Account.balance |
| `core/database/budget.sq` | 1-4 | `getTotalSpendingPoolBalance` sums static balances, not calculated ones |
| `core/database/Database.sq` | 5 | `balance` is a static INTEGER, not a derived value |

---

## Proposed Solution

**Strategy: Derive account balance from transactions (ACC-004 compliance)**

Two complementary changes are needed:

### 1. Update account balance when transaction status changes

Modify `UpdateTransactionStatusUseCase` to adjust the account balance when a transaction transitions to CONFIRMED or CANCELLED:
- **PENDING/OVERDUE -> CONFIRMED**: Deduct expense amount from (or add income amount to) the account balance via `AccountRepository.updateBalance()`
- **PENDING/OVERDUE -> CANCELLED**: No balance change needed (pending amounts were never applied to balance)

Similarly, `CreateTransactionUseCase` should NOT change the account balance (pending transactions are handled as reservations in STS).

### 2. Fix Safe to Spend formula for confirmed transactions

Either:
- **Option A (recommended):** Since confirming a transaction now updates the account balance (fix #1), the current STS formula `totalBalance - pending - overdue - ccReserved` will naturally work: `totalBalance` decreases when confirmed, pending decreases when confirmed, net effect is correct.
- **Option B (alternative):** Keep balance static and subtract `confirmedSpending` in the STS formula. This is simpler but violates ACC-004 and creates a confusing UX where the account card shows a number that never changes.

Option A is recommended because it satisfies ACC-004, produces an intuitive account balance display, and keeps the STS formula clean.

### 3. Account card rendering

No rendering code changes needed if Option A is implemented — `AccountCard` already reads `account.balance`, which will now be kept up to date. The `AccountViewModel` flow subscription will automatically re-emit when the balance changes in the DB.
