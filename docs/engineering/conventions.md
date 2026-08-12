# Conventions

Source of truth: current code under package `com.petergabriel.budgetcalendar`.

## Money

- Store/pass money as `Long` cents. Examples: `Account.balance`, `Transaction.amount`, `Transaction.signedAmount`, `BudgetSummary.*Amount` all use `Long`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/Account.kt:17-25`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/Transaction.kt:28-42`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/model/BudgetSummary.kt:3-12`.
- Format display with `CurrencyUtils.formatCents(...)`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/CurrencyUtils.kt:5-19`.
- DB monetary columns are `INTEGER`: `initial_balance`, `amount`, `signed_amount`, `rollover_amount`, notification monetary payloads: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-123`.

## Time

- Store timestamps/dates as `Long` Unix epoch millis. Examples: `Transaction.date`, `createdAt`, `updatedAt`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/Transaction.kt:28-42`.
- Use `DateUtils.nowMillis()`, `startOfDayMillis()`, month bounds, and UTC/local day conversion helpers instead of ad-hoc date math: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtils.kt:14-90`.
- Schema date/time fields are `INTEGER`: accounts/transactions/snapshots/notifications/settings use `created_at`/`updated_at` or date millis columns: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-123`.

## Transaction amounts

- `amount` = unsigned magnitude. `signed_amount` = accounting direction. Schema keeps both: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:14-29`.
- Default signs: income positive, expense negative, transfer source negative, transfer destination positive: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/CreateTransactionRequest.kt:3-28`.
- Balance/accounting reads should use `signed_amount`; reservation totals use magnitude `amount` where pending/overdue obligation size matters: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:62-78`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:16-33`.

## Transaction status

- In Kotlin, compare persisted status strings through `TransactionStatus.*.dbValue`; raw `"pending"`/`"overdue"`/`"confirmed"`/`"cancelled"` comparisons are forbidden outside enum conversion. Definitions and case-insensitive comparison: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/Transaction.kt:15-25`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/data/repository/BudgetRepositoryImpl.kt:48-62`.

## SQLDelight and DB IDs

- SQL belongs in `.sq` query files and numbered `.sqm` migrations under `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/`.
- Current tables use integer IDs; most domain IDs are `Long`. Core tables use `INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT` except singleton `user_settings.id INTEGER NOT NULL PRIMARY KEY DEFAULT 1`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-123`.
- Query methods insert/update via generated SQLDelight APIs, e.g. `insertTransaction`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq:42-58`.

## Koin DI

- DI is centralized in `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt`.
- Installed modules are `platformModule`, `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:106-220`.
- Repositories are `single`, use cases are `factory`, ViewModels use `viewModel`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:120-211`.

## Design system

- Design-token reference is `docs/engineering/design-system.html`; implementation source is `BudgetCalendarTheme`: colors, typography, spacing, radius, shadows: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/theme/BudgetCalendarTheme.kt:14-37`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/theme/BudgetCalendarTheme.kt:89-114`.
- Feature UI must use `BudgetCalendarTheme.colors`/`BcColors`; raw `Color(0x...)`, `Color.Black`, and `Color.White` literals are forbidden. Token definitions and feature usage: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/theme/Color.kt:8-54`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/CreditCardTile.kt:35-132`.
- Reusable components use `Bc*` names, e.g. `BcButton`, `BcBadge`, `BcHeader`, `BcTabBarPill`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcButton.kt:36-70`.
- Top-level app screens use `BcHeader` at `typography.headline`; title text is weighted, single-line, and ellipsized before its action slot: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcHeader.kt:23-56`. Each top-level screen passes the shared `BcNotificationBell` as its notification action: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:155-177`.
- `BcHeader.actionContent` carries `BcNotificationBell` alone; never wrap it with unrelated controls in a nested `Row`. Put Calendar month controls in a sibling row.
- `BcTabBarPill` and `BcSegmentedControl` expose `selectableGroup`, `Role.Tab`, and selected semantics; visual selection is never the only state signal: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcTabBarPill.kt:44-113`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcSegmentedControl.kt:31-64`.
- Interactive controls use `BudgetCalendarTheme.spacing.spacing12` (48dp) or larger. App-shell and migrated active-surface dimensions use spacing tokens; inactive Budget/Recurring screens remain outside this pass.
- Sheets use `BcSheetShell` with named heading, explicit close, scrollable body, reachable footer, and feature-owned dirty/confirmation state. Mutating sheets close only after success; failures preserve values and remain local.
- Form sheets use `BcInputGroup`, `BcSelectField` + `BcOptionPickerModal`, and `BcSwitchRow` instead of raw Material3 form controls. These components define field-level test semantics, modal selection semantics, switch role/state semantics, and 48dp interactive targets: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/form/BcInputGroup.kt:30-45`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/form/BcSelectField.kt:16-46`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcOptionPickerModal.kt:28-80`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/form/BcSwitchRow.kt:21-44`.
- This rule is applied in the migrated Account, credit-card, and recurring forms: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountFormSheet.kt:120-259`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardSettingsForm.kt:77-124`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardPaymentSheet.kt:95-145`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/recurring/presentation/components/RecurringFormSheet.kt:116-228`.
- Known gap: `BcInputGroup` exposes inline `errorText` but no supporting-text slot; character counters remain sibling `Text` nodes in the migrated forms rather than becoming a new public DS component: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/form/BcInputGroup.kt:40-44`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountFormSheet.kt:227-250`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/recurring/presentation/components/RecurringFormSheet.kt:216-234`.

## State and errors

- ViewModels expose read-only `StateFlow` and keep mutable state private: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountViewModel.kt:21-30`.
- Fallible use-case commands return `Result<T>` and validate before repository writes: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:13-44`.
- Split presentation failures by affected operation (month, projection, selected day, account/card list, settings, notification list, sandbox snapshot/projection/transactions/mutation). Preserve stale usable content instead of replacing unrelated sections with one broad error: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/sandbox/presentation/SandboxHomeUiState.kt:3-29`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/presentation/CalendarUiState.kt:3-29`.
