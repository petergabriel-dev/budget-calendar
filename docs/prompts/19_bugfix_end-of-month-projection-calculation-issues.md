# Bugfix: End of Month Projection Calculation Issues

**Bug Report:** `bug-reports/safe-to-spend/2026-04-01_1335_end-of-month-projection-calculation-issues.md`

**Affected Features:** Calendar / End of Month Projection

**Shadow Specs:**
- `specs/shadow/Calendar.shadow.md`
- `specs/shadow/Budget.shadow.md`
- `specs/shadow/Transaction.shadow.md`

---

## Tasks

- [ ] **Task 1: Read and understand `CalculateMonthProjectionUseCase`** — Read `CalculateMonthProjectionUseCase.kt` lines 1-50. Understand the formula: `poolBalance + signedTransactionSum` where `poolBalance` is the sum of spending pool account balances and `signedTransactionSum` is the sum of PENDING/OVERDUE transactions (income added, expenses subtracted). Note that `getMonthProjectionTransactions` only returns PENDING/OVERDUE transactions via `getPendingAndOverdueForSpendingPoolInRange`. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

- [ ] **Task 2: Verify `getMonthProjectionTransactions` query** — Read `transactions.sq` lines 95-104 (`getPendingAndOverdueForSpendingPoolInRange`). Confirm this query only returns PENDING and OVERDUE transactions (not CONFIRMED). This is by design — confirmed transactions should be reflected in `poolBalance` via `adjustBalance`. (Use Skill: kmp)

- [ ] **Task 3: Investigate TRANSFER handling** — In `CalculateMonthProjectionUseCase.kt` lines 39-45, TRANSFER returns `0L`. This means pending transfers are not counted in the projection. Determine if this is correct:
  - A TRANSFER from Spending Pool → Non-Spending Pool reduces available funds
  - A TRANSFER from Non-Spending Pool → Spending Pool increases available funds
  - Currently transfers are ignored in projection
  
  If transfers should be counted, the formula needs to handle them differently (subtract for outgoing, add for incoming). (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 4: Verify `adjustBalance` is called for confirmed transactions** — Read `UpdateTransactionStatusUseCase.kt` lines 31-47. Confirm that when a transaction is CONFIRMED:
  - EXPENSE: `adjustBalance(accountId, -amount)` is called
  - INCOME: `adjustBalance(accountId, +amount)` is called
  - TRANSFER: Both source (-amount) and destination (+amount) are adjusted
  
  If this is working, `poolBalance` should correctly reflect all confirmed transactions. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 5: Check for double-counting issues** — If `adjustBalance` is working, trace through a scenario:
  1. PENDING expense of $75 on spending pool account
  2. Confirm the expense
  3. After confirm: `poolBalance` decreases by $75 (via `adjustBalance`)
  4. After confirm: transaction is no longer PENDING, so not in `signedTransactionSum`
  5. Net effect: projection correctly reduced by $75
  
  If this trace shows double-counting or missing deduction, fix the formula. (Use Skill: kmp)

- [ ] **Task 6: Write reproduction tests** — Create test cases in `commonTest` that verify End of Month Projection:
  1. Create spending pool account with $500 balance, add PENDING expense of $75 → projection should be $425
  2. Confirm the expense → projection should remain $425
  3. Add PENDING income of $100 → projection should be $525
  4. Confirm the income → projection should remain $525
  (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

- [ ] **Task 7: Handle Credit Card expenses in projection (if needed)** — Per the bug report, CC expenses might not be counted in projection because CC accounts are typically not in the spending pool. Review whether CC payment TRANSFERs from spending pool should be counted when PENDING. Note: This may be out of scope if CC handling is tracked separately. (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

- [ ] **Task 8: Run full test suite** — Run `./gradlew :composeApp:allTests` to ensure no regressions. Verify all projection-related tests pass. (Use Skill: kmp)
