# Database Design Specification

> Budget Calendar - SQLDelight (SQLite) Schema Definition

## Overview

This document defines the database schema for the Budget Calendar Kotlin Multiplatform app. The database uses **SQLDelight** with **SQLite** for offline-first local storage.

### Design Principles

- **Offline-First**: All data stored locally; no network dependency
- **Idempotent Operations**: Safe to re-run, supports migration
- **Integer Storage**: Monetary values stored as cents (smallest unit)
- **Unix Timestamps**: All datetime columns stored as INTEGER (milliseconds since epoch)

---

## Schema Definition

### Tables

#### 1. accounts

Stores user financial accounts with type classification and spending pool designation.

```sql
-- Create accounts table
CREATE TABLE accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('checking', 'savings', 'credit_card', 'cash', 'investment')),
    balance INTEGER NOT NULL DEFAULT 0,
    is_in_spending_pool INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Index for fetching spending pool accounts
CREATE INDEX idx_accounts_spending_pool ON accounts(is_in_spending_pool);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique account identifier |
| name | TEXT | NOT NULL | Account display name |
| type | TEXT | NOT NULL, CHECK | Account type enum |
| balance | INTEGER | NOT NULL DEFAULT 0 | Current balance in cents |
| is_in_spending_pool | INTEGER | NOT NULL DEFAULT 0 | Boolean flag (0/1) |
| created_at | INTEGER | NOT NULL | Unix timestamp (ms) |
| updated_at | INTEGER | NOT NULL | Unix timestamp (ms) |

---

#### 2. transactions

Records all financial events with lifecycle state management.

```sql
-- Create transactions table
CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    destination_account_id INTEGER REFERENCES accounts(id),
    amount INTEGER NOT NULL,
    date INTEGER NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('income', 'expense', 'transfer')),
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'overdue', 'cancelled')),
    description TEXT,
    category TEXT,
    linked_transaction_id INTEGER REFERENCES transactions(id),
    is_sandbox INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Indexes for common query patterns
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_date ON transactions(date);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_sandbox ON transactions(is_sandbox);
CREATE INDEX idx_transactions_date_type ON transactions(date, type);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique transaction identifier |
| account_id | INTEGER | NOT NULL, FK → accounts(id) | Associated account |
| destination_account_id | INTEGER | FK → accounts(id) | For TRANSFER type - destination account |
| amount | INTEGER | NOT NULL | Amount in cents (negative for expense) |
| date | INTEGER | NOT NULL | Transaction date (Unix ms) |
| type | TEXT | NOT NULL, CHECK | Transaction type enum |
| status | TEXT | NOT NULL, CHECK, DEFAULT 'pending' | Lifecycle state |
| description | TEXT | | Optional transaction note |
| category | TEXT | | Optional categorization |
| linked_transaction_id | INTEGER | FK → transactions(id) | Links paired transfer transactions |
| is_sandbox | INTEGER | NOT NULL DEFAULT 0 | Sandbox isolation flag |
| created_at | INTEGER | NOT NULL | Unix timestamp (ms) |
| updated_at | INTEGER | NOT NULL | Unix timestamp (ms) |

**Transaction Type Values:**
- `income` — Money received (positive amount)
- `expense` — Money spent (negative amount)
- `transfer` — Between accounts (handled via two records)

**Transaction Status Values:**
- `pending` — Scheduled future transaction, reserves funds
- `confirmed` — Executed/verified transaction
- `overdue` — Passed due date without confirmation
- `cancelled` — Voided, funds released

---

#### 3. recurring_transactions

Manages scheduled/recurring income and expenses.

```sql
-- Create recurring_transactions table
CREATE TABLE recurring_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    day_of_month INTEGER NOT NULL CHECK (day_of_month >= 1 AND day_of_month <= 31),
    type TEXT NOT NULL CHECK (type IN ('income', 'expense', 'transfer')),
    description TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Index for finding active recurrences
CREATE INDEX idx_recurring_active ON recurring_transactions(is_active);
CREATE INDEX idx_recurring_day ON recurring_transactions(day_of_month);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique recurrence identifier |
| account_id | INTEGER | NOT NULL, FK → accounts(id) | Target account |
| amount | INTEGER | NOT NULL | Amount in cents |
| day_of_month | INTEGER | NOT NULL, CHECK 1-31 | Day of month to schedule |
| type | TEXT | NOT NULL, CHECK | Recurrence type |
| description | TEXT | | Optional label |
| is_active | INTEGER | NOT NULL DEFAULT 1 | Active flag (0/1) |
| created_at | INTEGER | NOT NULL | Unix timestamp (ms) |
| updated_at | INTEGER | NOT NULL | Unix timestamp (ms) |

---

#### 4. sandbox_snapshots

Stores What-If scenario snapshots for sandbox mode.

```sql
-- Create sandbox_snapshots table
CREATE TABLE sandbox_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    last_accessed_at INTEGER NOT NULL
);

-- Index for sorting by creation date
CREATE INDEX idx_snapshots_created ON sandbox_snapshots(created_at);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique snapshot identifier |
| name | TEXT | NOT NULL | Snapshot display name |
| created_at | INTEGER | NOT NULL | Unix timestamp (ms) |
| last_accessed_at | INTEGER | NOT NULL | Last accessed time for expiration tracking |

---

#### 5. sandbox_transactions

Stores transaction copies within a sandbox snapshot.

```sql
-- Create sandbox_transactions table
CREATE TABLE sandbox_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    snapshot_id INTEGER NOT NULL REFERENCES sandbox_snapshots(id) ON DELETE CASCADE,
    account_id INTEGER NOT NULL,
    amount INTEGER NOT NULL,
    date INTEGER NOT NULL,
    type TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    description TEXT,
    category TEXT,
    original_transaction_id INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Indexes for sandbox queries
CREATE INDEX idx_sandbox_snapshot ON sandbox_transactions(snapshot_id);
CREATE INDEX idx_sandbox_date ON sandbox_transactions(date);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique sandbox transaction ID |
| snapshot_id | INTEGER | NOT NULL, FK → sandbox_snapshots | Parent snapshot |
| account_id | INTEGER | NOT NULL | Copied account reference |
| amount | INTEGER | NOT NULL | Amount in cents |
| date | INTEGER | NOT NULL | Transaction date |
| type | TEXT | NOT NULL | Transaction type |
| status | TEXT | NOT NULL | Status |
| description | TEXT | | Description |
| category | TEXT | | Category |
| original_transaction_id | INTEGER | | Link to real transaction |
| created_at | INTEGER | NOT NULL | Unix timestamp (ms) |
| updated_at | INTEGER | NOT NULL | Unix timestamp (ms) |

---

#### 6. monthly_rollovers

Tracks month-to-month rollover amounts for continuous budget calculation.

```sql
-- Create monthly_rollovers table
CREATE TABLE monthly_rollovers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    rollover_amount INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    UNIQUE(year, month)
);

-- Index for rollover lookups
CREATE INDEX idx_rollovers_year_month ON monthly_rollovers(year, month);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique identifier |
| year | INTEGER | NOT NULL | Year (e.g., 2026) |
| month | INTEGER | NOT NULL | Month (1-12) |
| rollover_amount | INTEGER | NOT NULL | Amount rolled over in cents |
| created_at | INTEGER | NOT NULL | Unix timestamp (ms) |

---

#### 7. credit_card_settings

Stores credit card specific settings and tracking.

```sql
-- Create credit_card_settings table
CREATE TABLE credit_card_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    credit_limit INTEGER,
    statement_balance INTEGER,
    due_date INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Index for CC lookups by account
CREATE INDEX idx_cc_settings_account ON credit_card_settings(account_id);
```

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique identifier |
| account_id | INTEGER | NOT NULL, FK → accounts | Associated CC account |
| credit_limit | INTEGER | | Optional credit limit |
| statement_balance | INTEGER | | Current statement balance |
| due_date | INTEGER | | Next payment due date |
| created_at | INTEGER | NOT NULL | Unix timestamp (ms) |
| updated_at | INTEGER | NOT NULL | Unix timestamp (ms) |

---

## SQLDelight Query Definitions

### accounts

```sql
-- name: getAllAccounts :many
SELECT * FROM accounts ORDER BY created_at DESC;

-- name: getAccountById :one
SELECT * FROM accounts WHERE id = :id;

-- name: getSpendingPoolAccounts :many
SELECT * FROM accounts WHERE is_in_spending_pool = 1 ORDER BY name;

-- name: insertAccount :one
INSERT INTO accounts (name, type, balance, is_in_spending_pool, created_at, updated_at)
VALUES (:name, :type, :balance, :is_in_spending_pool, :created_at, :updated_at)
RETURNING *;

-- name: updateAccount :one
UPDATE accounts 
SET name = :name, type = :type, balance = :balance, 
    is_in_spending_pool = :is_in_spending_pool, updated_at = :updated_at
WHERE id = :id
RETURNING *;

-- name: deleteAccount :exec
DELETE FROM accounts WHERE id = :id;

-- name: getTotalSpendingPoolBalance :one
SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE is_in_spending_pool = 1;
```

### transactions

```sql
-- name: getTransactionsByAccount :many
SELECT * FROM transactions 
WHERE account_id = :account_id 
ORDER BY date DESC;

-- name: getTransactionsByDateRange :many
SELECT * FROM transactions 
WHERE date BETWEEN :startDate AND :endDate
ORDER BY date ASC;

-- name: getTransactionsByDate :many
SELECT * FROM transactions 
WHERE date = :date
ORDER BY created_at DESC;

-- name: getPendingTransactions :many
SELECT * FROM transactions 
WHERE status = 'pending' AND is_sandbox = 0
ORDER BY date ASC;

-- name: getOverdueTransactions :many
SELECT * FROM transactions 
WHERE status = 'overdue' AND is_sandbox = 0
ORDER BY date ASC;

-- name: getConfirmedTransactions :many
SELECT * FROM transactions 
WHERE status = 'confirmed' AND is_sandbox = 0
ORDER BY date DESC;

-- name: insertTransaction :one
INSERT INTO transactions (account_id, amount, date, type, status, description, category, is_sandbox, created_at, updated_at)
VALUES (:account_id, :amount, :date, :type, :status, :description, :category, :is_sandbox, :created_at, :updated_at)
RETURNING *;

-- name: updateTransactionStatus :one
UPDATE transactions 
SET status = :status, updated_at = :updated_at
WHERE id = :id
RETURNING *;

-- name: deleteTransaction :exec
DELETE FROM transactions WHERE id = :id;

-- name: getSafeToSpendAmount :one
SELECT COALESCE(SUM(t.amount), 0) 
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.is_in_spending_pool = 1 
AND t.status IN ('pending', 'confirmed')
AND t.is_sandbox = 0;

-- name: getCreditCardReservedAmount :one
SELECT COALESCE(SUM(t.amount), 0) 
FROM transactions t
JOIN accounts a ON t.account_id = a.id
WHERE a.type = 'credit_card'
AND t.status IN ('pending', 'confirmed')
AND t.is_sandbox = 0;
```

### recurring_transactions

```sql
-- name: getAllRecurringTransactions :many
SELECT * FROM recurring_transactions ORDER BY day_of_month;

-- name: getActiveRecurringTransactions :many
SELECT * FROM recurring_transactions WHERE is_active = 1 ORDER BY day_of_month;

-- name: getRecurringTransactionById :one
SELECT * FROM recurring_transactions WHERE id = :id;

-- name: insertRecurringTransaction :one
INSERT INTO recurring_transactions (account_id, amount, day_of_month, type, description, is_active, created_at, updated_at)
VALUES (:account_id, :amount, :day_of_month, :type, :description, :is_active, :created_at, :updated_at)
RETURNING *;

-- name: updateRecurringTransaction :one
UPDATE recurring_transactions 
SET account_id = :account_id, amount = :amount, day_of_month = :day_of_month,
    type = :type, description = :description, is_active = :is_active, updated_at = :updated_at
WHERE id = :id
RETURNING *;

-- name: deleteRecurringTransaction :exec
DELETE FROM recurring_transactions WHERE id = :id;
```

### sandbox_snapshots

```sql
-- name: getAllSnapshots :many
SELECT * FROM sandbox_snapshots ORDER BY created_at DESC;

-- name: getSnapshotById :one
SELECT * FROM sandbox_snapshots WHERE id = :id;

-- name: insertSnapshot :one
INSERT INTO sandbox_snapshots (name, created_at)
VALUES (:name, :created_at)
RETURNING *;

-- name: deleteSnapshot :exec
DELETE FROM sandbox_snapshots WHERE id = :id;
```

### sandbox_transactions

```sql
-- name: getSandboxTransactionsBySnapshot :many
SELECT * FROM sandbox_transactions 
WHERE snapshot_id = :snapshot_id
ORDER BY date ASC;

-- name: insertSandboxTransaction :one
INSERT INTO sandbox_transactions (snapshot_id, account_id, amount, date, type, status, description, category, original_transaction_id, created_at, updated_at)
VALUES (:snapshot_id, :account_id, :amount, :date, :type, :status, :description, :category, :original_transaction_id, :created_at, :updated_at)
RETURNING *;

-- name: deleteSandboxTransactionsBySnapshot :exec
DELETE FROM sandbox_transactions WHERE snapshot_id = :snapshot_id;
```

---

## Entity Relationships

```
┌─────────────────┐       ┌──────────────────┐
│    accounts     │       │ recurring_       │
│                 │       │ transactions     │
│ - id (PK)       │◄──────│ - account_id (FK)│
│ - name          │       │ - id (PK)        │
│ - type          │       └──────────────────┘
│ - balance       │
│ - is_in_spending│
│ - created_at    │       ┌──────────────────┐
│ - updated_at    │       │  transactions    │
└─────────────────┘       │                  │
       ▲                  │ - id (PK)        │
       │                  │ - account_id (FK)│
       │                  │ - amount         │
       │                  │ - date           │
       │                  │ - type           │
       │                  │ - status         │
       │                  │ - is_sandbox     │
       │                  │ - created_at     │
       │                  │ - updated_at    │
       │                  └──────────────────┘
       │                          ▲
       │                          │
       │                  ┌──────────────────────┐
       │                  │ sandbox_snapshots   │
       │                  │                      │
       └──────────────────│ - id (PK)            │
                          │ - name               │
                          │ - created_at        │
                          └──────────────────────┘
                                   ▲
                                   │
                          ┌──────────────────────┐
                          │ sandbox_transactions │
                          │                      │
                          │ - id (PK)            │
                          │ - snapshot_id (FK)   │
                          │ - original_          │
                          │   transaction_id     │
                          └──────────────────────┘
```

---

## Index Summary

| Index | Table | Columns | Purpose |
|-------|-------|---------|---------|
| idx_accounts_spending_pool | accounts | is_in_spending_pool | Filter spending pool |
| idx_transactions_account | transactions | account_id | Join/filter by account |
| idx_transactions_date | transactions | date | Calendar queries |
| idx_transactions_status | transactions | status | Lifecycle filtering |
| idx_transactions_sandbox | transactions | is_sandbox | Sandbox isolation |
| idx_transactions_date_type | transactions | date, type | Calendar summary |
| idx_recurring_active | recurring_transactions | is_active | Active filtering |
| idx_recurring_day | recurring_transactions | day_of_month | Day scheduling |
| idx_snapshots_created | sandbox_snapshots | created_at | Snapshot listing |
| idx_sandbox_snapshot | sandbox_transactions | snapshot_id | Sandbox loading |
| idx_sandbox_date | sandbox_transactions | date | Sandbox calendar |

---

## Migration Strategy

### Initial Schema (Version 1)

```sql
-- Version 1: Core schema
CREATE TABLE IF NOT EXISTS accounts (...);
CREATE TABLE IF NOT EXISTS transactions (...);
CREATE TABLE IF NOT EXISTS recurring_transactions (...);
CREATE TABLE IF NOT EXISTS sandbox_snapshots (...);
CREATE TABLE IF NOT EXISTS sandbox_transactions (...);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_accounts_spending_pool ON accounts(is_in_spending_pool);
CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_sandbox ON transactions(is_sandbox);
CREATE INDEX IF NOT EXISTS idx_recurring_active ON recurring_transactions(is_active);
```

### Note on SQLDelight Migrations

SQLDelight uses numbered migration files in the `src/commonMain/sqldelight/` directory:

```
sqldelight/
├── migrations/
│   ├── 1.sqm
│   └── 2.sqm
└── database/
    └── Database.sq
```

Each migration file (e.g., `1.sqm`) contains raw SQL statements executed in order.

---

## Type Mappings

| Kotlin Type | SQLite Type | Notes |
|-------------|-------------|-------|
| Long | INTEGER | Primary keys, timestamps |
| String | TEXT | Names, descriptions, enums |
| Boolean | INTEGER | Stored as 0/1 |
| Int | INTEGER | Day of month, amounts (cents) |

### Enum Storage

All enums (account type, transaction type/status) stored as TEXT strings:
- AccountType: `checking`, `savings`, `credit_card`, `cash`, `investment`
- TransactionType: `income`, `expense`, `transfer`
- TransactionStatus: `pending`, `confirmed`, `overdue`, `cancelled`

---

## Design Decisions

1. **Unix Timestamps**: Stored as INTEGER (milliseconds) for cross-platform consistency
2. **Cents for Money**: All monetary values stored as integers to avoid floating-point issues
3. **Sandbox Isolation**: `is_sandbox` flag on transactions enables What-If scenarios without data duplication
4. **Cascade Deletes**: Foreign key constraints use ON DELETE CASCADE for cleanup
5. **Auto-increment IDs**: SQLite INTEGER PRIMARY KEY provides efficient sequential IDs
