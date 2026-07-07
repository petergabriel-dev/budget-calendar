---
id: ADR-0010
title: Credit cards as accounts with live statement due
status: Active
date: 2026-07-07
---

# ADR-0010: Credit cards as accounts with live statement due

## Decision

- Credit cards are account tiles, but the tile leads with available credit when a limit exists, not total owed.
- Credit-card tiles show owed amount, live statement due, credit limit context, and utilization; text/icon accompany the card affordance so meaning is not color-only.
- Live statement due is display-only: `max(0, -(balance as of last statement close) - payments since close)`, with due date from `paymentDueAfter(payment_due_day, lastStatementClose(statement_closing_day))`.
- Total owed/current balance remains signed projected balance from ADR-0008. STS reserve remains projected unpaid balance from ADR-0006. Net worth remains unchanged.
- Creating a new credit-card account requires inline setup: credit limit `> 0`, statement closing day 1-31, payment due day 1-31. The settings row is saved atomically with account creation; settings failure rolls back the account.
- Add-card UI accepts "Current balance owed" as positive input and stores it once as negative `initial_balance`.
- Statement closing boundary is exclusive for charges: a charge dated on the closing day bills to the next statement. This corrects ADR-0009's previously inclusive wording/behavior.
- Existing credit cards may lack limit/cycle data; UI and summaries must render gracefully with null statement due/available credit instead of crashing. Backfill is out of scope.

## Why

- Credit-card users care first about remaining spendable credit, while total owed and statement due must remain visible as secondary context.
- Statement due differs from total owed: post-closing charges increase owed/current balance but must not inflate the prior statement due.
- Payments after closing should reduce statement due live without snapshot/cut automation.
- Mandatory setup prevents new cards from entering the system without a limit/cycle needed for the account-style tile and due display.
- Keeping due display-only preserves ADR-0006 and ADR-0008: no new ledger, STS, or net-worth paths.
- Exclusive closing boundary matches card-cycle semantics: closing day itself belongs to the next cycle for newly recorded charges.

## Affects

Docs:

- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/CreateAccountRequest.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:23-61`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountFormSheet.kt:159-219`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountScreen.kt:151-159`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/CreditCardTile.kt:38-130`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/credit_card_settings.sq:50-69`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtils.kt:92-125`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/model/CreditCardSummary.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummariesUseCase.kt:36-72`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummaryByIdUseCase.kt:26-69`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardDetailSheet.kt:59-86`
- `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/CreditCardUseCasesTest.kt:242-290`
- `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtilsStatementCycleTest.kt`
- `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:590-646`

Related ADRs:

- [ADR-0006](ADR-0006-credit-card-liability-balance-reservation.md)
- [ADR-0008](ADR-0008-unified-projected-credit-card-balance.md)
- [ADR-0009](ADR-0009-card-native-charge-sheet-statement-cycle.md)

## Consequences

- Good: credit cards read like credit-card accounts instead of generic liabilities.
- Good: statement due is live, payment-sensitive, and not inflated by post-closing charges.
- Good: new cards always have data needed for available-credit and statement-cycle UI.
- Good: net worth, STS reserve, and credit-card section total remain tied to existing signed projected balance math.
- Cost: summary use cases now need date-bounded card balance and payments-since queries.
- Cost: legacy cards can show incomplete limit/cycle UI until user edits settings.
- Risk: future work may confuse statement due with current owed; keep display-only wording and ADR-0006/ADR-0008 boundaries intact.

## Read when

- changing Accounts credit-card tile content, ordering, or totals
- changing statement due computation or statement due display
- changing credit-card account creation/setup requirements
- changing credit-card cycle boundary behavior
- changing card available-credit/utilization UI
- changing STS reserve, net-worth, signed projected balance, or credit-card section totals
- adding statement snapshots, minimum payments, due automation, or ledger-side statement logic

## Updates

- ADR-0009 remains active for card-native charge sheet and recurring cycle fields, but its closing-boundary model is corrected here: charge cutoff is exclusive, so a charge on closing day rolls to the next statement.
