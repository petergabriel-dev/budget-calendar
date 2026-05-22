# 37 — Fix Transfer Direction Display & Sandbox Promotion Bugs

**Feature:** Transfer display direction fix + sandbox transfer promotion fix
**Shadow Spec:** `specs/shadow/Transaction.shadow.md`
**Shadow Spec to Update:** `specs/shadow/TransactionModal.shadow.md` (already updated for Transfer support)
**Status:** Planned

---

The backend and ViewModel already fully support TRANSFER transactions — `CreateTransactionUseCase.createTransferPair()` creates two linked transactions with correct `signed_amount` values (-amount for source, +amount for destination). However, the `Transaction` domain model never reads `signed_amount` back from the database, forcing all UI consumers to re-derive the sign from `Transaction.type`. Since type alone cannot distinguish source (money out) from destination (money in) for transfers, all display logic shows transfers incorrectly.

Additionally, the sandbox data model (`SandboxTransaction`) lacks `destinationAccountId`, so promoting a sandbox TRANSFER creates a broken one-sided transaction instead of a linked pair.

---

## Section 1 — Non-Functional Requirements

- **Backward Compatibility:** DB migration must use safe defaults (`DEFAULT 0` for `signed_amount`, `NULL` for `destination_account_id`) so existing rows work without data migration.
- **Data Integrity:** `signed_amount` in every `transactions` row must be correct — source TRANSFER = negative, destination TRANSFER = positive, INCOME = positive, EXPENSE = negative.
- **Performance:** No meaningful performance impact — reading one additional column per SELECT is negligible.
- **Consistency:** All 5 display locations that derive sign from `type` must be replaced with `transaction.signedAmount`. No ad-hoc `when(type)` patterns should remain.
- **No Behavior Change:** INCOME and EXPENSE display must be identical to current behavior. Only TRANSFER direction display changes.

---

## Section 2 — Success Metrics

- **Functional:** Creating a transfer FROM account A shows `-₱X` in account A's transaction list and `+₱X` in account B's transaction list.
- **Functional:** Editing an existing transfer opens the form with correct type pre-populated.
- **Functional:** Promoting a sandbox TRANSFER creates a proper linked pair (two transactions with linkedTransactionId).
- **Functional:** Existing INCOME/EXPENSE transactions display identically before and after the change.
- **Quality:** All existing unit tests pass without modifications (except adding `signedAmount` to test data builders).
- **Quality:** `./gradlew :composeApp:compileCommonMainKotlinMetadata` passes with zero errors.
- **Quality:** `./gradlew :composeApp:testDebugUnitTest` passes with zero failures.

---

## Section 3 — Risks and Assumptions

### Assumptions

- All existing `transactions` rows have correct `signed_amount` values (written via `CreateTransactionRequest.defaultSignedAmount`). No data migration needed for the `transactions` table.
- `SandboxTransaction.signedAmount` is nullable because existing sandbox rows lack this column — the DDL uses `DEFAULT 0`.
- `AddSimulationTransactionUseCase` continues to block TRANSFER type in sandbox — no UI changes needed there.
- `RecurringTransaction` is a template (not a stored transaction), so `signedAmount` is computed as `-amount` for TRANSFER (source perspective), matching `CreateTransactionRequest.defaultSignedAmount`.
- `HomeScreen` upcoming transactions section may show any type — using `signedAmount` generically is correct.

### Risks

- **Risk: SQLDelight parameter order mismatch.** The `toEntity()` mapper uses positional parameters. If `signed_amount` is added to SELECT queries but the parameter order in `toEntity()` doesn't match, data will be silently misaligned. **Mitigation:** Task 1 and 2 must be done together; compile check after.
- **Risk: Test data builders missing `signedAmount`.** All test factories that create `Transaction` objects must be updated to include `signedAmount`. **Mitigation:** Compile check will catch this; search for `Transaction(` in test files.
- **Risk: PromoteTransactionUseCase TRANSFER partial failure.** If the destination insert succeeds but `updateLinkedTransactionId` fails, the source transaction is orphaned. **Mitigation:** This is consistent with existing `createTransferPair` behavior — same risk profile. Not introduced by this change.

---

## Section 3.5 — Context Package

Before starting each task, load:
- `specs/shadow/Transaction.shadow.md` — domain model, constraints, data flow
- `composeApp/src/commonMain/kotlin/.../features/transactions/domain/model/Transaction.kt`
- `composeApp/src/commonMain/kotlin/.../features/transactions/data/local/TransactionEntity.kt`
- `composeApp/src/commonMain/kotlin/.../features/transactions/data/mapper/TransactionMapper.kt`
- `composeApp/src/commonMain/kotlin/.../features/transactions/data/repository/TransactionRepositoryImpl.kt`
- `composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq`
- `composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq`

For Bug 2 tasks, also load:
- `composeApp/src/commonMain/kotlin/.../features/sandbox/domain/model/SandboxTransaction.kt`
- `composeApp/src/commonMain/kotlin/.../features/sandbox/data/local/SandboxTransactionsEntity.kt`
- `composeApp/src/commonMain/kotlin/.../features/sandbox/data/mapper/SandboxMapper.kt`
- `composeApp/src/commonMain/kotlin/.../features/sandbox/data/repository/SandboxRepositoryImpl.kt`
- `composeApp/src/commonMain/kotlin/.../features/sandbox/domain/repository/ISandboxRepository.kt`
- `composeApp/src/commonMain/kotlin/.../features/sandbox/domain/usecase/PromoteTransactionUseCase.kt`
- `composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/sandbox_transactions.sq`

---

## Section 4 — Tasks

### Task 1: Add `signedAmount` to Transaction domain model and entity

- [ ] In `TransactionEntity.kt`, add `val signedAmount: Long` field after `val amount: Long` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] In `Transaction.kt`, add `val signedAmount: Long` field after `val amount: Long` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] In `TransactionMapper.kt`, add `signedAmount = entity.signedAmount` mapping after the `amount` mapping (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] In `TransactionRepositoryImpl.kt`, add `signedAmount: Long` parameter to `toEntity()` function after `amount` parameter, and map it to `TransactionEntity(signedAmount = signedAmount)` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
  - **Given** `TransactionEntity` has `signedAmount` **When** `toEntity()` is called with 14 positional params **Then** the 5th parameter maps to `signedAmount` in the entity
  - **NFRs:** See Section 1
  - **Verification Gate:** Compile check passes after this task

### Task 2: Add `signed_amount` to all SQLDelight SELECT queries

- [ ] In `transactions.sq`, add `signed_amount` after `amount` in the column list of ALL 12 SELECT queries: `getTransactionsByAccount`, `getTransactionsByDateRange`, `getTransactionsByDate`, `getPendingTransactions`, `getOverdueTransactions`, `getConfirmedTransactions`, `getTransactionById`, `getTransactionByLinkedId`, `getPendingTransactionsForSpendingPool`, `getPendingAndOverdueForSpendingPoolInRange`, `getConfirmedTransactionsByDateRange`, `getPendingAndOverdueExpensesByAccount`. Queries using `t.` prefix must use `t.signed_amount`. (Use Skill: sqldelight-kmp) (Reference: specs/shadow/Transaction.shadow.md)
  - **Given** all SELECT queries omit `signed_amount` **When** column is added **Then** SQLDelight generates mapper with correct positional param
  - **NFRs:** Parameter order MUST match `toEntity()` function parameter order
  - **Checkpoint:** Commit before starting next task — run `./gradlew :composeApp:compileCommonMainKotlinMetadata`

### Task 3: Replace ad-hoc sign derivation in UI and use cases

- [ ] In `TransactionListItem.kt` (lines 41-51), replace `when` block with `val signedAmount = transaction.signedAmount`. Keep `amountColor` `when` block unchanged (INCOME=colorSuccess, EXPENSE=colorError, TRANSFER=colorInfo). Update `includePlusSign` to `transaction.signedAmount >= 0`. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] In `NotificationCenterSheet.kt` (lines 262-271, 362-369), replace `signedAmount(transaction)` private function body with `return transaction.signedAmount`. Update color logic at line ~267 to use `when` with TRANSFER=colorInfo. Update `includePlusSign` at line ~266 to `transaction.signedAmount > 0L`. (Use Skill: kmp)
- [ ] In `HomeScreen.kt` (lines 497, 504), replace hardcoded `"${CurrencyUtils.formatCents(transaction.amount)}"` and `"-${CurrencyUtils.formatCents(transaction.amount)}"` with `CurrencyUtils.formatCents(transaction.signedAmount)` for both lines. (Use Skill: kmp)
- [ ] In `CalculateMonthProjectionUseCase.kt` (lines 41-52), simplify TRANSFER branch to use `transaction.signedAmount`: keep pool-aware logic (destination in pool = 0L, else = `transaction.signedAmount`). Replace INCOME/EXPENSE branches with `transaction.signedAmount`. (Use Skill: kmp)
- [ ] In `GetSandboxSafeToSpendUseCase.kt` (lines 21-27), use `transaction.signedAmount ?: when(transaction.type) { ... }` fallback. (Use Skill: kmp)
- [ ] In `SandboxTransactionItem.kt` (lines 47-52), replace `when` block with `val signedAmount = transaction.signedAmount ?: when(transaction.type) { ... }` fallback. Update `amountColor` to use `when` with TRANSFER=colorInfo. Update badge (line 74) to handle TRANSFER with "Transfer" label and `BadgeVariant.Info`. (Use Skill: kmp)
- [ ] In `RecurringTransaction.kt` (lines 17-28), add computed `val signedAmount: Long` property: INCOME=amount, EXPENSE=-amount, TRANSFER=-amount. (Use Skill: kmp)
- [ ] In `RecurringCard.kt` (line 54), replace `when` block with `val signedAmount = recurring.signedAmount`. Update `includePlusSign` to `recurring.signedAmount > 0`. (Use Skill: kmp)
- [ ] In `FakeAccountRepository.kt` (lines 162-175), simplify `signedAmountFor()` to `return transaction.signedAmount`. (Use Skill: kmp)
- [ ] In `FakeSandboxRepository.kt` (lines 110-115), use `item.signedAmount ?: when(item.type) { ... }` fallback. (Use Skill: kmp)
  - **Given** `Transaction.signedAmount` exists **When** UI renders a transfer source transaction **Then** it shows negative amount in blue
  - **Given** `Transaction.signedAmount` exists **When** UI renders a transfer destination transaction **Then** it shows positive amount in blue
  - **NFRs:** See Section 1
  - **Verification Gate:** Compile check passes. Search codebase for remaining `when (transaction.type)` sign-derivation patterns — confirm zero remain in display code.

### Task 4: Add `destinationAccountId` and `signedAmount` to sandbox data model

- [ ] Create migration `9.sqm` with `ALTER TABLE sandbox_transactions ADD COLUMN destination_account_id INTEGER REFERENCES accounts(id) ON DELETE CASCADE;` and `ALTER TABLE sandbox_transactions ADD COLUMN signed_amount INTEGER NOT NULL DEFAULT 0;` (Use Skill: sqldelight-kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] In `Database.sq`, add `destination_account_id INTEGER REFERENCES accounts(id) ON DELETE CASCADE` and `signed_amount INTEGER NOT NULL DEFAULT 0` to `sandbox_transactions` DDL (Use Skill: sqldelight-kmp)
- [ ] In `SandboxTransaction.kt`, add `val destinationAccountId: Long? = null` and `val signedAmount: Long? = null` fields (Use Skill: kmp)
- [ ] In `SandboxTransactionsEntity.kt`, add `val destinationAccountId: Long?` and `val signedAmount: Long?` fields (Use Skill: kmp)
- [ ] In `SandboxMapper.kt`, add `destinationAccountId` and `signedAmount` mappings (Use Skill: kmp)
- [ ] In `sandbox_transactions.sq`, add `destination_account_id` and `signed_amount` to `getSandboxTransactionsBySnapshot`, `getSandboxTransactionById`, and `insertSandboxTransaction` column lists (Use Skill: sqldelight-kmp)
- [ ] In `ISandboxRepository.kt`, add `destinationAccountId: Long? = null` and `signedAmount: Long? = null` parameters to `insertSandboxTransaction` (Use Skill: kmp)
- [ ] In `SandboxRepositoryImpl.kt`, update `insertSandboxTransaction` to pass new fields to SQL. Update `toTransactionEntity()` to include `destinationAccountId` and `signedAmount` parameters. (Use Skill: kmp)
- [ ] In `AddSimulationTransactionUseCase.kt`, NO LOGIC CHANGE — keep TRANSFER block. (No code change needed, just confirming out of scope) (Reference: specs/shadow/Transaction.shadow.md)
  - **Given** DB migration adds columns with safe defaults **When** existing sandbox data is read **Then** `destinationAccountId` is null and `signedAmount` is 0
  - **NFRs:** See Section 1
  - **Checkpoint:** Commit before starting next task

### Task 5: Fix PromoteTransactionUseCase for TRANSFER

- [ ] In `PromoteTransactionUseCase.kt`, inject `CreateTransactionUseCase` as a dependency. For TRANSFER type, delegate to `createTransactionUseCase` which handles linked pair creation via `createTransferPair`. For INCOME/EXPENSE, keep `transactionRepository.createTransaction` with `sandboxTransaction.signedAmount ?: <fallback>`. For TRANSFER without `destinationAccountId`, return failure with "Transfer requires a destination account" error. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
  - **Given** a sandbox TRANSFER transaction with `destinationAccountId` set **When** promoted **Then** a linked pair of real transactions is created with correct `signedAmount` values
  - **Given** a sandbox TRANSFER transaction with `destinationAccountId` null **When** promoted **Then** it fails with "Transfer requires a destination account" error
  - **Given** a sandbox INCOME or EXPENSE transaction **When** promoted **Then** a single real transaction is created with correct `signedAmount`
  - **NFRs:** See Section 1
  - **Verification Gate:** Compile check passes

### Task 6: Update test data and add transfer promotion tests

- [ ] Update all test factories that construct `Transaction(...)` to include `signedAmount` field. Search for `Transaction(` in `commonTest` directory and add `signedAmount` to each constructor call. (Use Skill: kmp)
- [ ] Add test to `TransactionUseCasesTest.kt`: "promote sandbox TRANSFER creates linked pair" — create a `SandboxTransaction` with `destinationAccountId` set, promote it via `PromoteTransactionUseCase`, verify two linked `Transaction` records exist. (Use Skill: kmp)
- [ ] Add test to `TransactionUseCasesTest.kt`: "promote sandbox TRANSFER without destination fails". (Use Skill: kmp)
- [ ] Add test to `TransactionUseCasesTest.kt`: "transfer source transaction has negative signedAmount, destination has positive signedAmount" — verify `signedAmount` is correctly read back from DB. (Use Skill: kmp)
  - **Given** `Transaction.signedAmount` field exists **When** test data is constructed **Then** all `Transaction(...)` calls include `signedAmount`
  - **NFRs:** All existing tests must pass without regressions

### Task 7: Verify and commit

- [ ] Run `./gradlew :composeApp:compileCommonMainKotlinMetadata` — zero errors
- [ ] Run `./gradlew :composeApp:testDebugUnitTest` — zero failures
- [ ] Search entire codebase for remaining `when (transaction.type)` sign-derivation patterns — confirm zero remain in display code
- [ ] Verify no ad-hoc `transaction.amount` with hardcoded `-` prefix remains in display code

---

## Section 5 — Definition of Done

- [ ] All 7 tasks above are implemented and committed
- [ ] Transfer source transactions show `-₱X` in blue in TransactionListItem
- [ ] Transfer destination transactions show `+₱X` in blue in TransactionListItem
- [ ] All INCOME and EXPENSE display is unchanged from before the fix
- [ ] `Transaction.signedAmount` is used directly in all 5 display locations
- [ ] Sandbox TRANSFER promotion creates a proper linked pair
- [ ] DB migration `9.sqm` is safe for existing data (DEFAULT values)
- [ ] All existing tests pass without regressions
- [ ] `./gradlew :composeApp:compileCommonMainKotlinMetadata` passes
- [ ] `./gradlew :composeApp:testDebugUnitTest` passes
- [ ] No `when(transaction.type)` sign-derivation patterns remain in display composables or use cases