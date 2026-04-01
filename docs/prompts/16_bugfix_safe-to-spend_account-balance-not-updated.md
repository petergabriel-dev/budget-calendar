# Bugfix: Account Balance Not Reflecting Transactions / Safe to Spend Confirm Bug

**Bug Report:** `bug-reports/safe-to-spend/2026-04-01_1200_account-balance-not-reflecting-transactions.md`

**Affected Features:** Account Management, Transaction Management, Safe to Spend Calculation

**Shadow Specs:**
- `specs/shadow/Account.shadow.md`
- `specs/shadow/Transaction.shadow.md`
- `specs/shadow/Budget.shadow.md`

---

## Tasks

- [ ] **Task 1: Write reproduction tests** — Create test cases in `commonTest` that prove the two bugs: (1) account balance doesn't change after creating/confirming a transaction, (2) Safe to Spend increases when a pending expense is confirmed. Tests should cover: create PENDING expense → verify STS deducts → confirm expense → verify STS stays deducted and account balance decreases. Also test CANCELLED restores STS. These tests should fail before the fix and pass after. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md, specs/shadow/Budget.shadow.md)

- [ ] **Task 2: Add `adjustBalance` SQL query** — Add an `adjustAccountBalance` query to `accounts.sq` that atomically increments/decrements an account's balance: `UPDATE accounts SET balance = balance + :delta, updated_at = :updated_at WHERE id = :id`. This avoids full-row updates and race conditions. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 3: Expose `adjustBalance` through `IAccountRepository` and `AccountRepositoryImpl`** — Add `suspend fun adjustBalance(accountId: Long, delta: Long)` to the repository interface and implement it using the new SQL query from Task 2. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 4: Update `UpdateTransactionStatusUseCase` to adjust account balance on CONFIRMED** — When a transaction transitions to CONFIRMED: for EXPENSE, call `adjustBalance(accountId, -amount)`; for INCOME, call `adjustBalance(accountId, +amount)`. For TRANSFER, adjust both source (-amount) and destination (+amount). Inject `IAccountRepository` into the use case. This aligns with the Transaction shadow spec's "Confirm Transaction Flow" which requires `RecalculateSafeToSpend` after confirm. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 5: Handle CANCELLED transitions in `UpdateTransactionStatusUseCase`** — When a PENDING or OVERDUE transaction is CANCELLED, no balance adjustment is needed (pending amounts were never applied to balance — they're handled as STS reservations). However, if a CONFIRMED transaction could be cancelled (currently blocked by state machine), that would need a reversal. Verify the state machine prevents CONFIRMED → CANCELLED and add a comment documenting why no balance adjustment is needed for cancel. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 6: Register `IAccountRepository` dependency in `UpdateTransactionStatusUseCase` Koin wiring** — Update `KoinModules.kt` to inject `IAccountRepository` into `UpdateTransactionStatusUseCase`'s factory definition. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 7: Verify Safe to Spend formula correctness** — With account balance now updated on CONFIRMED, trace through the STS formula: `totalBalance - pending - overdue - ccReserved`. When PENDING → CONFIRMED: `totalBalance` decreases by amount (Task 4), `pendingReservations` decreases by amount (status change). Net STS change = 0, which is correct (money was already reserved). Confirm this with the reproduction tests from Task 1. No code change expected — this task is a verification checkpoint. (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

- [ ] **Task 8: Run full test suite and verify** — Run `./gradlew :composeApp:allTests` to ensure no regressions. Verify the reproduction tests from Task 1 now pass. (Use Skill: kmp)
