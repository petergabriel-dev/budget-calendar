# Phase 1 Tests: Core Financial Loop

> Unit tests for Account, Transaction, and Budget use-case validations and state transitions

**Shadow Specs:** specs/shadow/Account.shadow.md, specs/shadow/Transaction.shadow.md, specs/shadow/Budget.shadow.md

---

## Group A: Test Infrastructure

- [x] Create `FakeAccountRepository` implementing `IAccountRepository` with in-memory `MutableList<Account>` in `commonTest/kotlin/.../features/accounts/` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Create `FakeTransactionRepository` implementing `ITransactionRepository` with in-memory `MutableList<Transaction>` in `commonTest/kotlin/.../features/transactions/` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Create `FakeBudgetRepository` implementing `IBudgetRepository` with in-memory rollover list in `commonTest/kotlin/.../features/budget/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

---

## Group B: Account Use Case Tests

- [x] Test `CreateAccountUseCase`: valid CHECKING account created with correct fields; name empty → error; name > 50 chars → error; non-CC with negative balance → error; CREDIT_CARD with negative balance → allowed (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Test `UpdateAccountUseCase`: valid name update persisted; non-existent account id → error (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Test `DeleteAccountUseCase`: account with zero transactions → deleted; account with existing transactions → error "Cannot delete account with existing transactions" (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Test `GetSpendingPoolAccountsUseCase`: only accounts with `isInSpendingPool = true` returned; empty pool → empty list (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Test `CalculateNetWorthUseCase`: assets summed, CC liabilities subtracted; all assets → positive; all liabilities → negative; empty accounts → zero (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

---

## Group C: Transaction Use Case Tests

- [x] Test `CreateTransactionUseCase` — valid INCOME: amount > 0, date not in past, status PENDING, persisted correctly (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Test `CreateTransactionUseCase` — valid EXPENSE: amount > 0, date ≤ today + 30 days, persisted correctly (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Test `CreateTransactionUseCase` — TRANSFER: creates two linked transactions (debit + credit), `linkedTransactionId` set on both; same-account transfer → error "Cannot transfer to the same account" (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Test `CreateTransactionUseCase` — validation failures: amount ≤ 0 → error; INCOME with past date → error "Cannot record income in the past"; EXPENSE > 30 days future → error "Cannot schedule expenses more than 30 days ahead" (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Test `UpdateTransactionStatusUseCase` — valid transitions: PENDING → CONFIRMED; PENDING → CANCELLED; OVERDUE → CONFIRMED; OVERDUE → CANCELLED (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Test `UpdateTransactionStatusUseCase` — invalid transitions: CONFIRMED → PENDING → error; CANCELLED → CONFIRMED → error (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Test `DeleteTransactionUseCase`: deleting a TRANSFER also deletes linked transaction via `linkedTransactionId`; deleting INCOME/EXPENSE only removes that transaction (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Test `MarkOverdueTransactionsUseCase`: PENDING transaction with `date + 1 day < currentDate` → status updated to OVERDUE; PENDING transaction with future date → status unchanged; CONFIRMED transaction → untouched (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

---

## Group D: Budget Use Case Tests

- [x] Test `CalculateSpendingPoolUseCase`: `availableToSpend = poolBalance - pendingSum - overdueSum`; no pool accounts → zero; cancelled transactions excluded from deduction (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `GetSafeToSpendUseCase`: emits correct long value matching spending pool calculation; updates when transactions change (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `GetBudgetSummaryUseCase`: `BudgetSummary` fields populated correctly — `totalLiquidAssets`, `pendingReservations`, `overdueReservations`, `availableToSpend` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `SaveRolloverUseCase` and `GetRolloverHistoryUseCase`: rollover saved for (year, month); duplicate (year, month) → upsert or error per constraint; history ordered by year DESC, month DESC (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Test `ApplyRolloverUseCase`: previous month's rollover added to current Safe to Spend; no prior rollover → no change (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
