# KMP Reference: SQLDelight Database

## File Placement

SQLDelight `.sq` files live in a mirrored package path under the `sqldelight` source root:

```
commonMain/
└── sqldelight/
    └── com/petergabriel/budgetcalendar/
        ├── Account.sq
        ├── Transaction.sq
        ├── RecurringTransaction.sq
        ├── SandboxSnapshot.sq
        ├── SandboxTransaction.sq
        ├── MonthlyRollover.sq
        └── CreditCardSettings.sq
```

The generated Kotlin interfaces are in the package `com.petergabriel.budgetcalendar`.
The database class is `BudgetCalendarDatabase`.

---

## Storage Conventions

| Data Type | SQLite Column Type | Kotlin Type | Example |
|-----------|-------------------|-------------|---------|
| Money | `INTEGER NOT NULL` | `Long` (cents) | `15000L` = ₱150.00 |
| Timestamp | `INTEGER NOT NULL` | `Long` (Unix ms) | `1711497600000L` |
| Boolean | `INTEGER NOT NULL DEFAULT 0` | `Boolean` (mapped) | `0` = false |
| Enum | `TEXT NOT NULL` | `String` | `"checking"` |
| Optional string | `TEXT` | `String?` | nullable |
| ID (internal) | `INTEGER PRIMARY KEY AUTOINCREMENT` | `Long` | auto |

**Never use `REAL` or `NUMERIC` for money.** Floating-point precision loss is a financial bug.

---

## Schema Conventions

```sql
-- Account.sq
CREATE TABLE accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('checking', 'savings', 'credit_card', 'cash', 'investment')),
    balance INTEGER NOT NULL DEFAULT 0,
    is_in_spending_pool INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_accounts_spending_pool ON accounts(is_in_spending_pool);
```

Rules:
- Every table has `created_at` and `updated_at` as `INTEGER NOT NULL`
- Use `CHECK` constraints on enum TEXT columns
- Add indexes for every column used in `WHERE` clauses
- No `REAL` columns — map decimals to cents in the application layer

---

## Query Naming Conventions

```sql
-- Account.sq

-- name suffix determines generated return type:
-- :many  → List<T>
-- :one   → T (throws if not found) or T? (nullable)
-- :exec  → Unit (no return)
-- :insert → Long (last inserted rowId) — use RETURNING * for full entity

getAllAccounts:
SELECT * FROM accounts ORDER BY created_at DESC;

getAccountById:
SELECT * FROM accounts WHERE id = :id;

getSpendingPoolAccounts:
SELECT * FROM accounts WHERE is_in_spending_pool = 1 ORDER BY name;

insertAccount:
INSERT INTO accounts (name, type, balance, is_in_spending_pool, created_at, updated_at)
VALUES (:name, :type, :balance, :is_in_spending_pool, :created_at, :updated_at)
RETURNING *;

updateAccount:
UPDATE accounts
SET name = :name, type = :type, balance = :balance,
    is_in_spending_pool = :is_in_spending_pool, updated_at = :updated_at
WHERE id = :id
RETURNING *;

deleteAccount:
DELETE FROM accounts WHERE id = :id;

getTotalSpendingPoolBalance:
SELECT COALESCE(SUM(balance), 0) AS total FROM accounts WHERE is_in_spending_pool = 1;
```

---

## Mapper Pattern

Mappers live in `data/mapper/`. They are extension functions — not classes with state.

```kotlin
// features/accounts/data/mapper/AccountMapper.kt
fun Accounts.toDomain(): Account = Account(
    id = id,
    name = name,
    type = AccountType.fromString(type),
    balance = balance,
    isInSpendingPool = is_in_spending_pool == 1L,
    createdAt = created_at,
    updatedAt = updated_at
)

fun CreateAccountRequest.toInsertParams(now: Long): AccountInsertParams = AccountInsertParams(
    name = name,
    type = type.value,
    balance = initialBalance,
    is_in_spending_pool = if (isInSpendingPool) 1L else 0L,
    created_at = now,
    updated_at = now
)
```

Rules:
- `DatabaseEntity.toDomain()` — DB → domain model
- `DomainRequest.toInsertParams()` — domain request → DB insert params
- Never put business logic in mappers — they only translate field names and types
- Boolean stored as `Long` (0L/1L) in SQLite; mapper converts to `Boolean`

---

## Database Setup

```kotlin
// core/database/Database.kt (commonMain)
expect fun createBudgetCalendarDatabase(driver: SqlDriver): BudgetCalendarDatabase

// androidMain/core/database/Database.android.kt
actual fun createBudgetCalendarDatabase(driver: SqlDriver): BudgetCalendarDatabase =
    BudgetCalendarDatabase(driver)

// iosMain/core/database/Database.ios.kt
actual fun createBudgetCalendarDatabase(driver: SqlDriver): BudgetCalendarDatabase =
    BudgetCalendarDatabase(driver)
```

The `SqlDriver` is injected by the platform-specific Koin module. See `references/di.md`.

---

## Reactive Queries (Flow)

SQLDelight 2.x supports reactive queries via `.asFlow()`:

```kotlin
// In repository implementation
override fun observeAll(): Flow<List<Account>> =
    queries.getAllAccounts()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entities -> entities.map { it.toDomain() } }
```

Use `mapToList(Dispatchers.IO)` for list queries, `mapToOne(Dispatchers.IO)` for single-row queries.
Always specify `Dispatchers.IO` — never query on the main thread.

---

## Migration Strategy

Migration files are in:
```
commonMain/sqldelight/migrations/
├── 1.sqm    # Initial schema
├── 2.sqm    # Subsequent changes
```

Each `.sqm` file contains raw SQL executed in order. Schema version is managed by SQLDelight.

```sql
-- 1.sqm (initial schema — CREATE TABLE statements)
CREATE TABLE accounts (...);
CREATE TABLE transactions (...);
-- etc.
```

When adding a column in a migration:
```sql
-- 2.sqm
ALTER TABLE accounts ADD COLUMN currency TEXT NOT NULL DEFAULT 'PHP';
```

Never modify an existing `.sqm` file once shipped. Always add a new numbered file.
