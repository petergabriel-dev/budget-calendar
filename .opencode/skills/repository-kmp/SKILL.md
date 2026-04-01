---
name: repository-kmp
description: Repository pattern implementation for Kotlin Multiplatform with Clean Architecture
---

# Skill: Repository KMP

## Project Context

- **Framework/Language**: Kotlin 1.9.22, Kotlin Multiplatform
- **Architecture**: Clean Architecture + MVVM
- **Project**: Budget Calendar

## Conventions

### Folder Structure (Package by Feature)

```
src/
├── features/
│   ├── accounts/
│   │   ├── data/
│   │   │   ├── mapper/
│   │   │   │   └── AccountMapper.kt
│   │   │   └── repository/
│   │   │       └── AccountRepositoryImpl.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── Account.kt
│   │   │   ├── repository/
│   │   │   │   └── IAccountRepository.kt
│   │   │   └── usecase/
│   │   │       ├── CreateAccountUseCase.kt
│   │   │       ├── GetAccountsUseCase.kt
│   │   │       ├── UpdateAccountUseCase.kt
│   │   │       └── DeleteAccountUseCase.kt
│   │   └── presentation/
│   │       └── viewmodel/
│   │           └── AccountViewModel.kt
│   └── transactions/
│       └── ...
```

### Naming Conventions

- **Models**: PascalCase (e.g., `Account`, `Transaction`)
- **Repository Interfaces**: `I` prefix + PascalCase (e.g., `IAccountRepository`)
- **Repository Implementations**: PascalCase + `Impl` suffix (e.g., `AccountRepositoryImpl`)
- **Mappers**: PascalCase + `Mapper` suffix (e.g., `AccountMapper`)
- **Use Cases**: Verb + Entity + `UseCase` (e.g., `GetAccountsUseCase`, `CreateTransactionUseCase`)

### Domain Layer (Interface-First)

```kotlin
// src/features/accounts/domain/model/Account.kt
package com.budgetcalendar.features.accounts.domain.model

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Long, // Stored in cents
    val colorHex: String?,
    val iconName: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

enum class AccountType {
    CHECKING,
    SAVINGS,
    CREDIT,
    CASH,
    INVESTMENT
}
```

```kotlin
// src/features/accounts/domain/repository/IAccountRepository.kt
package com.budgetcalendar.features.accounts.domain.repository

import com.budgetcalendar.features.accounts.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface IAccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getAccountById(id: String): Flow<Account?>
    suspend fun insertAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: String)
    fun getActiveAccounts(): Flow<List<Account>>
    fun getAccountsByType(type: String): Flow<List<Account>>
}
```

### Data Layer (Implementation)

```kotlin
// src/features/accounts/data/mapper/AccountMapper.kt
package com.budgetcalendar.features.accounts.data.mapper

import com.budgetcalendar.database.budget_calendardb.Account
import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.model.AccountType

object AccountMapper {
    fun toDomain(dbAccount: Account): Account {
        return Account(
            id = dbAccount.id,
            name = dbAccount.name,
            type = AccountType.valueOf(dbAccount.type.uppercase()),
            balance = dbAccount.balance,
            colorHex = dbAccount.colorHex,
            iconName = dbAccount.iconName,
            isActive = dbAccount.isActive == 1L,
            createdAt = dbAccount.createdAt,
            updatedAt = dbAccount.updatedAt
        )
    }

    fun toDbModel(account: Account): Account {
        return Account(
            id = account.id,
            name = account.name,
            type = account.type.name.lowercase(),
            balance = account.balance,
            colorHex = account.colorHex,
            iconName = account.iconName,
            isActive = if (account.isActive) 1L else 0L,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}
```

```kotlin
// src/features/accounts/data/repository/AccountRepositoryImpl.kt
package com.budgetcalendar.features.accounts.data.repository

import app.cash.sqldelight.coroutines.flowFlow
import com.budgetcalendar.database.budget_calendardb.AccountQueries
import com.budgetcalendar.features.accounts.data.mapper.AccountMapper
import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AccountRepositoryImpl(
    private val accountQueries: AccountQueries
) : IAccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountQueries.getAllAccounts()
            .flowFlow(Dispatchers.IO)
            .map { accounts -> accounts.map { AccountMapper.toDomain(it) } }
    }

    override fun getAccountById(id: String): Flow<Account?> {
        return accountQueries.getAccountById(id)
            .flowFlow(Dispatchers.IO)
            .map { account -> account?.let { AccountMapper.toDomain(it) } }
    }

    override suspend fun insertAccount(account: Account): Unit = withContext(Dispatchers.IO) {
        accountQueries.insertAccount(
            id = account.id,
            name = account.name,
            type = account.type.name.lowercase(),
            balance = account.balance,
            colorHex = account.colorHex,
            iconName = account.iconName,
            isActive = if (account.isActive) 1L else 0L,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }

    override suspend fun updateAccount(account: Account): Unit = withContext(Dispatchers.IO) {
        accountQueries.updateAccount(
            name = account.name,
            type = account.type.name.lowercase(),
            balance = account.balance,
            colorHex = account.colorHex,
            iconName = account.iconName,
            updatedAt = account.updatedAt,
            id = account.id
        )
    }

    override suspend fun deleteAccount(id: String): Unit = withContext(Dispatchers.IO) {
        accountQueries.deleteAccount(id)
    }

    override fun getActiveAccounts(): Flow<List<Account>> {
        return accountQueries.getActiveAccounts()
            .flowFlow(Dispatchers.IO)
            .map { accounts -> accounts.map { AccountMapper.toDomain(it) } }
    }

    override fun getAccountsByType(type: String): Flow<List<Account>> {
        return accountQueries.getAccountsByType(type)
            .flowFlow(Dispatchers.IO)
            .map { accounts -> accounts.map { AccountMapper.toDomain(it) } }
    }
}
```

## Use Cases

```kotlin
// src/features/accounts/domain/usecase/GetAccountsUseCase.kt
package com.budgetcalendar.features.accounts.domain.usecase

import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(
    private val repository: IAccountRepository
) {
    operator fun invoke(): Flow<List<Account>> {
        return repository.getAllAccounts()
    }

    fun getActive(): Flow<List<Account>> {
        return repository.getActiveAccounts()
    }

    fun getById(id: String): Flow<Account?> {
        return repository.getAccountById(id)
    }
}
```

```kotlin
// src/features/accounts/domain/usecase/CreateAccountUseCase.kt
package com.budgetcalendar.features.accounts.domain.usecase

import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.model.AccountType
import com.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import java.util.UUID

class CreateAccountUseCase(
    private val repository: IAccountRepository
) {
    suspend operator fun invoke(
        name: String,
        type: AccountType,
        balance: Long = 0L,
        colorHex: String? = null,
        iconName: String? = null
    ): Account {
        val now = System.currentTimeMillis()
        val account = Account(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            balance = balance,
            colorHex = colorHex,
            iconName = iconName,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        
        repository.insertAccount(account)
        return account
    }
}
```

## Restrictions

- **ALWAYS** define repository interfaces in the domain layer
- **ALWAYS** implement repositories in the data layer
- **ALWAYS** use `Flow` for reactive data streams from queries
- **ALWAYS** use `withContext(Dispatchers.IO)` for database operations
- **ALWAYS** use mapper classes to convert between domain models and database entities
- **ALWAYS** use UUIDs for primary keys (String type in SQLDelight)
- **NEVER** expose database entities outside of the data layer
- **ALWAYS** inject repository interfaces into use cases, not implementations
- **ALWAYS** handle nullable returns gracefully with `Flow<Nullable>` or null safety
