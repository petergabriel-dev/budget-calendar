---
name: sqldelight-kmp
description: SQLDelight database setup, queries, and migrations for Kotlin Multiplatform
---

# Skill: SQLDelight KMP

## Project Context

- **Framework/Language**: Kotlin 1.9.22, Kotlin Multiplatform
- **Platform**: iOS (Apple Silicon), Android
- **Database**: SQLDelight 2.0.1 with SQLite
- **Project**: Budget Calendar

## Conventions

### Folder Structure

```
src/
├── core/
│   └── database/
│       ├── src/
│       │   ├── commonMain/
│       │   │   └── sqldelight/
│       │   │       └── budget_calendardb/
│       │   │           ├── Accounts.sq
│       │   │           ├── Transactions.sq
│       │   │           └── migrations/
│       │   │               └── 1_initial.sqm
│       │   ├── androidMain/
│       │   └── iosMain/
│       └── build.gradle.kts
```

### Naming Conventions

- **.sq files**: PascalCase, descriptive (e.g., `Accounts.sq`, `Transactions.sq`)
- **Queries**: `camelCase`, verb-prefixed (e.g., `getAccountById`, `insertTransaction`)
- **Tables**: `snake_case`, plural (e.g., `accounts`, `transactions`)
- **Columns**: `snake_case` (e.g., `account_id`, `created_at`)
- **Migrations**: `N_description.sqm` (e.g., `1_initial.sqm`, `2_add_sandbox.sqm`)

### Database Schema Patterns

```sql
-- Example: Accounts table
CREATE TABLE accounts (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    balance INTEGER NOT NULL DEFAULT 0,
    color_hex TEXT,
    icon_name TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Example: Transactions table
CREATE TABLE transactions (
    id TEXT PRIMARY KEY NOT NULL,
    account_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    description TEXT,
    category TEXT,
    date INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_transactions_date ON transactions(date);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_status ON transactions(status);
```

### Query File Organization

```sql
-- Accounts.sq
getAllAccounts:
SELECT * FROM accounts WHERE is_active = 1 ORDER BY name;

getAccountById:
SELECT * FROM accounts WHERE id = :id;

insertAccount:
INSERT INTO accounts (id, name, type, balance, color_hex, icon_name, is_active, created_at, updated_at)
VALUES (:id, :name, :type, :balance, :colorHex, :iconName, :isActive, :createdAt, :updatedAt);

updateAccount:
UPDATE accounts SET name = :name, type = :type, balance = :balance, color_hex = :colorHex, icon_name = :iconName, updated_at = :updatedAt
WHERE id = :id;

deleteAccount:
DELETE FROM accounts WHERE id = :id;
```

## Component/Function Structure

### Database Factory (Common)

```kotlin
// src/core/database/src/commonMain/kotlin/com/budgetcalendar/database/DatabaseFactory.kt
package com.budgetcalendar.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryExecutors
import com.budgetcalendar.database.budget_calendardb.BudgetCalendarDatabase

expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}

object DatabaseFactory {
    private var database: BudgetCalendarDatabase? = null

    fun getInstance(factory: DatabaseDriverFactory): BudgetCalendarDatabase {
        return database ?: synchronized(this) {
            database ?: BudgetCalendarDatabase(
                driver = factory.createDriver(),
                queryExecutor = QueryExecutors.SYNCHRONIZED
            ).also { database = it }
        }
    }
}
```

### Driver Factories (Platform-Specific)

```kotlin
// src/core/database/src/androidMain/kotlin/com/budgetcalendar/database/DatabaseDriverFactory.kt
package com.budgetcalendar.database

import android.content.Context
import app.cash.sqldelight.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            context = context,
            name = "budget_calendardb"
        )
    }
}
```

```kotlin
// src/core/database/src/iosMain/kotlin/com/budgetcalendar/database/DatabaseDriverFactory.kt
package com.budgetcalendar.database

import app.cash.sqldelight.native.NativeSqliteDriver
import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            name = "budget_calendardb"
        )
    }
}
```

## Restrictions

- **ALWAYS** use `expect/actual` for platform-specific database drivers
- **ALWAYS** use `INTEGER` for timestamps (Unix epoch millis)
- **ALWAYS** use `INTEGER` for amounts (store as cents/millis)
- **ALWAYS** create indexes on foreign keys and frequently queried columns
- **NEVER** expose raw SQL outside of `.sq` query files
- **ALWAYS** use parameterized queries (`:param`) to prevent SQL injection
- **ALWAYS** include `created_at` and `updated_at` columns in all tables
- **ALWAYS** use `ON DELETE CASCADE` for foreign keys to maintain referential integrity
