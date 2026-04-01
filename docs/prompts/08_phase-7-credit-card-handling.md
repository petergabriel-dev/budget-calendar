# Phase 7: Credit Card Handling

> Liability account management with reserved payment tracking, Safe to Spend integration, and anti-double-deduction logic

**Shadow Spec:** specs/shadow/CreditCard.shadow.md

---

## Group A: Database & SQLDelight

- [x] Create a numbered `.sqm` migration file that adds `credit_card_settings` table (columns: `id` INTEGER PK AUTOINCREMENT, `account_id` INTEGER NOT NULL FK → `accounts(id)` ON DELETE CASCADE, `credit_limit` INTEGER nullable, `statement_balance` INTEGER nullable, `due_date` INTEGER nullable, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL; index: `idx_cc_settings_account` on `account_id`) — already present in existing migration `3.sqm` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Create `credit_card_settings.sq` SQLDelight file in `core/database/` with queries: `getAllCreditCardSettings`, `getCreditCardSettingsByAccountId`, `insertCreditCardSettings`, `updateCreditCardSettings`, `deleteCreditCardSettings` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Add `getCreditCardReservedAmount` query to `credit_card_settings.sq`: `SELECT COALESCE(SUM(amount), 0) AS reserved FROM transactions WHERE account_id = :accountId AND type = 'expense' AND status IN ('pending', 'overdue') AND is_sandbox = 0` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Add `getAllCreditCardReservedAmounts` query to `credit_card_settings.sq`: joins `transactions` with `accounts` where `type = 'credit_card'`, groups by `account_id`, sums pending/overdue expense amounts (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Add `getPendingAndOverdueExpensesByAccount` query to `transactions.sq`: `SELECT * FROM transactions WHERE account_id = :accountId AND type = 'expense' AND status IN ('pending', 'overdue') AND is_sandbox = 0 ORDER BY date ASC` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

---

## Group B: Domain Layer

- [x] Define `CreditCardSettings` data class in `features/creditcard/domain/model/` with fields: `id: Long`, `accountId: Long`, `creditLimit: Long?`, `statementBalance: Long?`, `dueDate: Long?`, `createdAt: Long`, `updatedAt: Long` — all monetary fields as `Long` (cents) (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Define `CreditCardSummary` data class in `features/creditcard/domain/model/` with fields: `accountId: Long`, `accountName: String`, `currentBalance: Long`, `reservedAmount: Long`, `statementBalance: Long?`, `creditLimit: Long?`, `availableCredit: Long?`, `dueDate: Long?` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Define `ICreditCardRepository` interface in `features/creditcard/domain/repository/` with: `getAllSettings(): Flow<List<CreditCardSettings>>`, `getSettingsByAccountId(accountId: Long): Result<CreditCardSettings>`, `insert(accountId: Long, creditLimit: Long?, statementBalance: Long?, dueDate: Long?): Result<CreditCardSettings>`, `update(accountId: Long, creditLimit: Long?, statementBalance: Long?, dueDate: Long?): Result<CreditCardSettings>`, `delete(accountId: Long): Result<Unit>`, `getReservedAmount(accountId: Long): Result<Long>`, `getAllReservedAmounts(): Result<Map<Long, Long>>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `CreateCreditCardSettingsUseCase`: validates that the account exists and has `type = CREDIT_CARD`; validates `creditLimit >= 0` if provided; inserts via `ICreditCardRepository.insert` with optional fields; returns `Result<CreditCardSettings>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `GetCreditCardSettingsUseCase`: retrieves settings by `accountId`; if no settings row exists, auto-creates one with null optional fields via `CreateCreditCardSettingsUseCase`; returns `Result<CreditCardSettings>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `UpdateCreditCardSettingsUseCase`: validates `creditLimit >= 0` if provided; calls `ICreditCardRepository.update`; returns `Result<CreditCardSettings>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `CalculateReservedAmountUseCase`: calls `ICreditCardRepository.getReservedAmount(accountId)` which sums pending/overdue expenses on the CC; returns `Result<Long>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `GetCreditCardSummariesUseCase`: collects `ICreditCardRepository.getAllSettings()` as `Flow`; for each setting, fetches account via `IAccountRepository`, computes reserved amount via `CalculateReservedAmountUseCase`, computes `availableCredit = creditLimit - abs(currentBalance)` (null if no limit); maps to `List<CreditCardSummary>`; returns `Flow<List<CreditCardSummary>>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `GetCreditCardSummaryByIdUseCase`: fetches settings + account + reserved amount for a single CC; returns `Result<CreditCardSummary>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `MakeCreditCardPaymentUseCase`: validates `amount > 0`, source account exists and is not the CC, destination account exists and has `type = CREDIT_CARD`; delegates to existing `CreateTransactionUseCase` with `type = TRANSFER`, `accountId = sourceAccountId`, `destinationAccountId = ccAccountId`; returns `Result<Transaction>` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

---

## Group C: Data Layer

- [x] Implement `CreditCardMapper` in `features/creditcard/data/mapper/`: converts SQLDelight `Credit_card_settings` entity → `CreditCardSettings` domain model (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `CreditCardRepositoryImpl` implementing `ICreditCardRepository` in `features/creditcard/data/repository/`: delegates all operations to SQLDelight queries from `credit_card_settings.sq`; wraps results with `CreditCardMapper`; emits `Flow` via `.asFlow()` on list queries; `getReservedAmount` delegates to `getCreditCardReservedAmount` query; `getAllReservedAmounts` delegates to `getAllCreditCardReservedAmounts` query (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

---

## Group D: Safe to Spend Integration

- [x] Update `CalculateSafeToSpendUseCase` to include CC expense deductions: when calculating Safe to Spend, pending/overdue EXPENSE transactions on CC accounts (not in spending pool) must still be subtracted; CC payment TRANSFERs from spending pool to CC must NOT be double-deducted (CC-005 anti-double-deduction rule) (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

---

## Group E: Dependency Injection

- [x] Register `ICreditCardRepository` → `CreditCardRepositoryImpl` (singleton) in Koin `repositoryModule` in `di/KoinModules.kt` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Register all credit card use cases (`CreateCreditCardSettingsUseCase`, `GetCreditCardSettingsUseCase`, `UpdateCreditCardSettingsUseCase`, `CalculateReservedAmountUseCase`, `GetCreditCardSummariesUseCase`, `GetCreditCardSummaryByIdUseCase`, `MakeCreditCardPaymentUseCase`) as `factory { }` in Koin `useCaseModule` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

---

## Group F: Presentation Layer

- [x] Define `CreditCardUiState` data class in `features/creditcard/presentation/`: `creditCards: List<CreditCardSummary>`, `selectedCard: CreditCardSummary?`, `selectedCardSettings: CreditCardSettings?`, `isLoading: Boolean`, `isPaymentSheetVisible: Boolean`, `suggestedPaymentAmount: Long`, `error: String?` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `CreditCardViewModel` in `features/creditcard/presentation/`: exposes `CreditCardUiState` via `StateFlow`; collects `GetCreditCardSummariesUseCase` on init; provides `selectCard(accountId)`, `openPaymentSheet()`, `closePaymentSheet()`, `makePayment(sourceAccountId, amount)`, `updateSettings(request)` methods; sets `suggestedPaymentAmount = selectedCard.reservedAmount` when payment sheet opens (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Register `CreditCardViewModel` in Koin `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

---

## Group G: UI Components

- [x] Implement `CreditCardListItem` composable: card showing CC name, current balance (negative, red), reserved amount badge (`CVw4C`), due date if set; uses `XOueE` (Card/Dark) styling; taps to select card (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `CreditCardDetailSheet` composable: bottom sheet showing CC summary — balance, reserved amount, statement balance, credit limit, available credit, due date; "Make Payment" primary button (`mbxe4`); "Edit Settings" outline button (`xR72O`); transaction list of pending/overdue expenses using `Ks9uf` (Transaction Item) (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `CreditCardPaymentSheet` composable: bottom sheet with `L2zpZ` (Large Amount Input) pre-filled with reserved amount as suggested payment; dropdown/selector for source account (spending pool accounts only); confirmation warning if amount > reserved; confirm/cancel buttons; calls `onConfirmPayment(sourceAccountId, amount)` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Implement `CreditCardSettingsForm` composable: form with `c9XFF` (Input Group) fields for credit limit (optional, cents), statement balance (optional, cents), due date (optional, date picker); save/cancel buttons; inline validation for negative credit limit (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Integrate CC components into the Accounts screen: show CC accounts with `CreditCardListItem` in a separate "Credit Cards" section; tapping a CC opens `CreditCardDetailSheet`; wire navigation between detail → payment → settings flows (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Hook into account creation flow: when a new account is created with `type = CREDIT_CARD`, automatically call `CreateCreditCardSettingsUseCase` to initialize the settings row (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)

---

## Group H: Tests

- [x] Create `FakeCreditCardRepository` implementing `ICreditCardRepository` with configurable in-memory `MutableList<CreditCardSettings>` and a `reservedAmounts: MutableMap<Long, Long>` in `commonTest/kotlin/.../features/creditcard/` (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Test `CreateCreditCardSettingsUseCase`: valid CC account → creates settings; non-CC account type → error; account not found → error (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Test `GetCreditCardSettingsUseCase`: existing settings → returns them; no settings row → auto-creates with null optionals (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Test `UpdateCreditCardSettingsUseCase`: valid update → persists changes; negative credit limit → validation error (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Test `CalculateReservedAmountUseCase`: no pending expenses → returns 0; multiple pending/overdue expenses → correct sum; confirmed/cancelled expenses excluded (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Test `GetCreditCardSummariesUseCase`: multiple CCs → independent summaries; CC with credit limit → availableCredit computed; CC without limit → availableCredit is null (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Test `MakeCreditCardPaymentUseCase`: valid payment → creates TRANSFER; amount ≤ 0 → error; source == destination → error; destination not CC → error; amount > reserved → allowed (no error) (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
- [x] Test Safe to Spend integration: CC expense deducts from Safe to Spend; CC payment TRANSFER does NOT double-deduct; confirm CC payment → reserved decreases (Use Skill: kmp) (Reference: specs/shadow/CreditCard.shadow.md)
