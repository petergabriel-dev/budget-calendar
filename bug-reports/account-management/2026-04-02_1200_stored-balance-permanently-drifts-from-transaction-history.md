# Bug: Stored Account Balance Permanently Drifts from Transaction History

**Date:** 2026-04-02
**Feature:** Account Management + Safe to Spend
**Status:** ROOT CAUSE CONFIRMED — architectural fix required

---

## Description

Even after the prompt-24 fixes (`flatMapLatest` + `adjustBalance()` in `CreateTransactionUseCase`), transactions created before the fix show stale account balances and STS values in the UI. The fix is reactive — it correctly re-queries the database — but the database itself contains the wrong value.

**Observed state:**
- Account "Savings" created with ₱500
- Grocery expense -₱100 (CONFIRMED) — created *before* prompt-24 fix
- Transport expense -₱50 (CONFIRMED) — created *after* prompt-24 fix
- Expected account balance: ₱350
- Actual account balance shown in UI: ₱450

The -₱50 was correctly applied (prompt-24 `CreateTransactionUseCase` fix). The -₱100 was never applied because it was created before that fix existed.

---

## Root Cause

The `balance` column on the `accounts` table is a mutable running total. Every code path that creates or confirms a transaction must explicitly call `accountRepository.adjustBalance()` to keep it correct. This is inherently fragile:

1. Any new code path that forgets `adjustBalance()` silently corrupts the balance.
2. Already-corrupt rows in the DB cannot be repaired without knowing which transactions "missed" their adjustment — there is no audit trail.
3. The `flatMapLatest` fix (prompt 24, tasks 2 & 3) correctly makes flows reactive, but reactive queries against a wrong stored value just surface the wrong value faster.

### Timeline of corruption

| Event | Expected balance | Actual `accounts.balance` in DB |
|-------|-----------------|--------------------------------|
| Account created ₱500 | ₱500 | ₱500 |
| Grocery -₱100 CONFIRMED (before fix) | ₱400 | ₱500 — `adjustBalance()` never called |
| Transport -₱50 CONFIRMED (after fix) | ₱350 | ₱450 — only this one applied |

### Why previous fixes didn't fully resolve this

- **Prompt 22 / 23 (`combine` → `flatMapLatest`):** Correct reactive plumbing, but re-queries the wrong stored value.
- **Prompt 24 task 4 (`adjustBalance()` in `CreateTransactionUseCase`):** Only applies to transactions created after the fix was deployed. Pre-existing data is unaffected.

---

## Affected Files

| File | Issue |
|------|-------|
| `core/database/Database.sq` | `accounts.balance` is a mutable running total |
| `features/accounts/data/repository/AccountRepositoryImpl.kt` | `adjustBalance()` mutates stored balance; any missed call causes drift |
| `features/transactions/domain/usecase/CreateTransactionUseCase.kt` | `adjustBalance()` added in prompt 24 — fixes future creates only |
| `features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt` | `adjustBalance()` called on confirm — correct but still fragile |

---

## Proposed Solution: Option B1 — Computed Balance

Replace the mutable `balance` column with an immutable `initial_balance` column (set once at account creation, never mutated). Compute the runtime balance on every read via SQL:

```
balance = initial_balance
        + SUM(amount WHERE type='income' AND status='confirmed')
        - SUM(amount WHERE type='expense' AND status='confirmed')
        ± SUM(amount WHERE type='transfer' AND status='confirmed')  -- see note
```

**Transfer direction note:** Each transfer creates two linked transactions (`account_id` = source and `account_id` = destination). Since both have `type='transfer'`, direction must be encoded explicitly. The recommended approach is adding a `signed_amount` column (set at INSERT time: income = +amount, expense = −amount, transfer source = −amount, transfer destination = +amount). This makes the balance SQL trivial: `initial_balance + SUM(signed_amount WHERE status='confirmed')`.

**Why this is correct:**
- Pre-existing -₱100 Grocery transaction IS in the DB with `status='confirmed'` → it will be included in the computed balance automatically, repairing stale data with zero manual intervention.
- Future missed `adjustBalance()` calls are impossible — the method no longer exists.
- Balance is always the authoritative ground truth derived from the ledger.

### Schema migration (7.sqm)

```sql
-- Rename balance → initial_balance (immutable from now on)
ALTER TABLE accounts RENAME COLUMN balance TO initial_balance;

-- Add signed_amount to transactions for efficient balance computation
ALTER TABLE transactions ADD COLUMN signed_amount INTEGER NOT NULL DEFAULT 0;

-- Backfill signed_amount for existing transactions
UPDATE transactions SET signed_amount = amount WHERE type = 'income';
UPDATE transactions SET signed_amount = -amount WHERE type = 'expense';
-- Transfer direction: requires knowing source vs destination per linked pair
-- Source is the transaction that was NOT created as the linked copy:
-- (heuristic: lower id in a linked pair = source = debit)
UPDATE transactions SET signed_amount = -amount 
  WHERE type = 'transfer' AND id < linked_transaction_id;
UPDATE transactions SET signed_amount = amount 
  WHERE type = 'transfer' AND id > linked_transaction_id;
```

### Code changes required

1. Remove `adjustAccountBalance` SQL query from `accounts.sq`
2. Add `getAccountsWithComputedBalance` and `getTotalSpendingPoolComputedBalance` queries using `initial_balance + SUM(signed_amount WHERE status='confirmed')`
3. Remove `adjustBalance()` from `IAccountRepository` and `AccountRepositoryImpl`
4. Remove `adjustBalance()` calls from `UpdateTransactionStatusUseCase` and `CreateTransactionUseCase`
5. Remove `IAccountRepository` dependency from `CreateTransactionUseCase` (no longer needed)
6. `AccountRepositoryImpl` switches its trigger from `balanceChangedTrigger` to `transactionChangedTrigger` (depends on `ITransactionRepository`)
7. `BudgetRepositoryImpl.getTotalSpendingPoolBalance()` uses the new computed query
8. Update Koin wiring throughout

---

## Verification

After fix, the pre-existing -₱100 Grocery transaction must automatically appear in the computed balance without any manual data repair. Expected result: Savings account shows ₱350 (₱500 − ₱100 − ₱50).
