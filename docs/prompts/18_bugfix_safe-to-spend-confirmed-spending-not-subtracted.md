# Bugfix: Safe to Spend Not Subtracting Confirmed Spending

**Bug Report:** `bug-reports/safe-to-spend/2026-04-01_1330_safe-to-spend-not-subtracting-confirmed-spending.md`

**Affected Features:** Budget / Safe to Spend Calculation

**Shadow Specs:**
- `specs/shadow/Budget.shadow.md`
- `specs/shadow/Account.shadow.md`
- `specs/shadow/Transaction.shadow.md`

---

## Tasks

- [ ] **Task 1: Investigate `confirmedSpending` usage in `CalculateSafeToSpendUseCase`** — Read `CalculateSafeToSpendUseCase.kt` lines 46-58. Note that `confirmedSpending` is calculated and stored in `BudgetSummary` but never subtracted from `availableToSpend`. Determine whether this is dead code (if `adjustBalance` works correctly, confirmed transactions are already reflected in `totalBalance`) or if it needs to be subtracted. (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

- [ ] **Task 2: Verify `adjustBalance` behavior** — Run the existing test `confirmExpense_deductsAccountBalance` in `TransactionUseCasesTest.kt`. This test already verifies that confirming an EXPENSE calls `adjustBalance(accountId, -amount)`. If this test passes, `adjustBalance` IS working and `totalBalance` should reflect confirmed transactions. If it fails, `adjustBalance` is not working. (Use Skill: kmp)

- [ ] **Task 3: Determine correct STS formula** — Based on Task 2 findings:
  - **If `adjustBalance` IS working:** The current STS formula `totalBalance - pending - overdue - ccReserved` should be correct because `totalBalance` already reflects confirmed transactions via `adjustBalance`. The `confirmedSpending` calculation (lines 50-52) is dead code and should be removed from the `combine` block.
  - **If `adjustBalance` IS NOT working:** The formula should subtract `confirmedSpending` to account for confirmed expenses not yet reflected in `totalBalance`. Add `confirmedSpending` subtraction to line 26-31.
  Document the decision in code comments. (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

- [ ] **Task 4: Implement the fix** — Based on Task 3 decision:
  - **Option A (remove dead code):** Remove the `combine` block at lines 46-58 that calculates and stores `confirmedSpending`, since confirmed transactions are already reflected in `totalBalance`.
  - **Option B (subtract confirmed spending):** Modify the formula at lines 26-31 to: `totalBalance - pendingReservations - overdueReservations - creditCardReserved - confirmedSpending`. Then ensure the `combine` block at 46-58 correctly accumulates `confirmedSpending`.
  (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

- [ ] **Task 5: Write/update reproduction tests** — If tests don't already exist, add test cases to `TransactionUseCasesTest.kt` or `BudgetUseCasesTest.kt` that verify:
  1. Create PENDING expense → STS correctly reduces by pending amount
  2. Confirm the expense → STS correctly stays stable (balance deducted, pending cleared)
  3. Add another PENDING expense → STS reduces again
  4. Confirm → STS stays stable
  These tests should pass after the fix. (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)

- [ ] **Task 6: Run full test suite** — Run `./gradlew :composeApp:allTests` to ensure no regressions. Verify all STS-related tests pass. (Use Skill: kmp)
