# Bugfix: End of Month Projection Doesn't Reflect Confirmed Transactions

**Bug Report:** `bug-reports/calendar/2026-04-01_1400_end-of-month-projection-account-balance-not-updating.md`

**Affected Features:** Calendar / End of Month Projection

**Shadow Specs:**
- `specs/shadow/Calendar.shadow.md`
- `specs/shadow/Budget.shadow.md`
- `specs/shadow/Transaction.shadow.md`

---

## Tasks

- [ ] **Task 1: Write reproduction test for `CalculateMonthProjectionUseCase`** — Create a test in `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/CalendarProjectionUseCasesTest.kt` that verifies the End of Month Projection updates when an account balance changes. Test: create spending pool account with $500 balance, add $75 PENDING expense in current month, verify projection = $425, then simulate balance update to $425, verify projection = $425. Use `FakeAccountRepository` and `FakeTransactionRepository` patterns from existing tests. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

- [ ] **Task 2: Investigate SQLDelight Flow emission after `adjustAccountBalance`** — In `AccountRepositoryImpl.kt`, the `getSpendingPoolAccounts()` uses `database.accountsQueries.getSpendingPoolAccounts(::toEntity).asFlow().mapToList(dispatcher)`. The `asFlow()` from SQLDelight should emit when the database changes. Add a test or logging to verify that calling `adjustAccountBalance` triggers a re-emission of `getSpendingPoolAccounts()`. If it does not emit, this confirms the Flow observation is not working. Read `AccountRepositoryImpl.kt` lines 41-47 and `accounts.sq` lines 11-15. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [ ] **Task 3: Fix `CalculateMonthProjectionUseCase` Flow subscription** — The current `combine()` at lines 30-33 passes `accountRepository.getSpendingPoolAccounts()` directly. If the Flow from SQLDelight is not re-emitting after `adjustAccountBalance`, restructure to use `flatMapLatest` to explicitly re-query when accounts change: `accountRepository.getSpendingPoolAccounts().flatMapLatest { accounts -> transactionRepository.getMonthProjectionTransactions(...).map { transactions -> calculate(accounts, transactions) } }`. Read `CalculateMonthProjectionUseCase.kt` lines 30-55 and restructure the Flow to ensure it re-evaluates when `getSpendingPoolAccounts()` emits. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

- [ ] **Task 4: Verify `UpdateTransactionStatusUseCase` calls `adjustBalance()`** — Add instrumentation or confirm via existing test that when a PENDING EXPENSE is CONFIRMED, `accountRepository.adjustBalance()` is called with `-amount`. Read `UpdateTransactionStatusUseCase.kt` lines 31-47 and verify the `adjustBalance` call path. If the call is not happening, fix the flow to ensure `adjustBalance` is invoked. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [ ] **Task 5: Run reproduction test and verify fix** — Run the test created in Task 1 to verify the fix works. The projection should correctly update when account balance changes via `adjustBalance()`. If test fails, iterate on Tasks 2-4. Run command: `./gradlew :composeApp:jvmTest --tests "CalendarProjectionUseCasesTest"` from `src/mobile/`. (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
