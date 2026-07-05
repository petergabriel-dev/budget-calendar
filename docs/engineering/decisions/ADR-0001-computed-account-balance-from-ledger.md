---
id: ADR-0001
title: Computed account balance from ledger
status: Active
date: 2026-07-06
---

# ADR-0001: Computed account balance from ledger

## Decision

- Account balance is derived at read time: `accounts.initial_balance + SUM(transactions.signed_amount)` for confirmed, non-sandbox transactions.
- `accounts.balance` mutable running total is superseded by immutable `initial_balance` plus ledger rows.
- Transaction direction lives in `transactions.signed_amount`; `amount` remains unsigned magnitude.

## Why

- Stored balances drifted when old or missed transaction code paths skipped balance adjustment: `bug-reports/account-management/2026-04-02_1200_stored-balance-permanently-drifts-from-transaction-history.md:11-27`.
- Migration 7 converted `balance` to `initial_balance` and backfilled `signed_amount`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/7.sqm:1-23`.
- Current reads compute from ledger, so confirmed historical rows repair balance on read: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:62-78`.

## Affects

Docs:

- `docs/engineering/architecture.md`
- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`

Code:

- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-29`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:6-22`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:47-78`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:1-14`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/7.sqm:1-23`

## Consequences

- Good: no balance mutation path can be forgotten.
- Good: confirmed ledger corrections immediately change derived balances.
- Cost: balance queries join transactions and must keep status/sandbox filters correct.
- Risk: wrong `signed_amount` at insert/migration corrupts every computed consumer.

## Read when

- changing account balance, STS, net-worth, projection, or transaction sign logic
- adding transaction types or status values
- editing SQLDelight account/budget queries

## Supersedes

- Mutable `accounts.balance` running total and `adjustBalance()`-style balance mutation.
