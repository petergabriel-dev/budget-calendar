---
name: koin-kmp
description: Koin dependency injection configuration for Kotlin Multiplatform
---

# Skill: Koin KMP

## Project Context

- **Framework/Language**: Kotlin 1.9.22, Kotlin Multiplatform
- **Platform**: iOS, Android
- **DI Framework**: Koin 3.5.3
- **Project**: Budget Calendar

## Conventions

### Folder Structure

```
src/
├── core/
│   └── di/
│       ├── src/
│       │   ├── commonMain/
│       │   │   └── kotlin/
│       │   │       └── com/budgetcalendar/di/
│       │   │           ├── AppModules.kt
│       │   │           ├── DatabaseModule.kt
│       │   │           ├── RepositoryModule.kt
│       │   │           ├── UseCaseModule.kt
│       │   │           └── ViewModelModule.kt
│       │   ├── androidMain/
│       │   │   └── kotlin/
│       │   │       └── com/budgetcalendar/di/
│       │   │           └── AndroidPlatformModule.kt
│       │   └── iosMain/
│       │       └── kotlin/
│       │           └── com/budgetcalendar/di/
│       │               └── IosPlatformModule.kt
│       └── build.gradle.kts
```

### Naming Conventions

- **Module files**: PascalCase, descriptive (e.g., `DatabaseModule.kt`, `RepositoryModule.kt`)
- **Module names**: String constants (e.g., `"database"`, `"repository"`)
- **Factory functions**: Verb-prefixed (e.g., `provideDatabase`, `provideAccountRepository`)

### Module Organization

```kotlin
// src/core/di/src/commonMain/kotlin/com/budgetcalendar/di/AppModules.kt
package com.budgetcalendar.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object AppModules {
    val database = databaseModule
    val repository = repositoryModule
    val useCase = useCaseModule
    val viewModel = viewModelModule

    val app = listOf(database, repository, useCase, viewModel)
}
```

```kotlin
// src/core/di/src/commonMain/kotlin/com/budgetcalendar/di/DatabaseModule.kt
package com.budgetcalendar.di

import com.budgetcalendar.database.BudgetCalendarDatabase
import com.budgetcalendar.database.DatabaseDriverFactory
import com.budgetcalendar.database.DatabaseFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val databaseModule = module {
    single { DatabaseDriverFactory(get()) }
    single { DatabaseFactory.getInstance(get()) }
    single { get<BudgetCalendarDatabase>().accountQueries }
    single { get<BudgetCalendarDatabase>().transactionQueries }
}
```

### Repository Module

```kotlin
// src/core/di/src/commonMain/kotlin/com/budgetcalendar/di/RepositoryModule.kt
package com.budgetcalendar.di

import com.budgetcalendar.features.accounts.data.repository.AccountRepositoryImpl
import com.budgetcalendar.features.accounts.domain.repository.AccountRepository
import com.budgetcalendar.features.transactions.data.repository.TransactionRepositoryImpl
import com.budgetcalendar.features.transactions.domain.repository.TransactionRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::AccountRepositoryImpl) { bind<AccountRepository>() }
    singleOf(::TransactionRepositoryImpl) { bind<TransactionRepository>() }
}
```

### Use Case Module

```kotlin
// src/core/di/src/commonMain/kotlin/com/budgetcalendar/di/UseCaseModule.kt
package com.budgetcalendar.di

import com.budgetcalendar.features.accounts.domain.usecase.CreateAccountUseCase
import com.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.budgetcalendar.features.accounts.domain.usecase.UpdateAccountUseCase
import com.budgetcalendar.features.accounts.domain.usecase.DeleteAccountUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::GetAccountsUseCase)
    singleOf(::CreateAccountUseCase)
    singleOf(::UpdateAccountUseCase)
    singleOf(::DeleteAccountUseCase)
}
```

### ViewModel Module (Android)

```kotlin
// src/core/di/src/androidMain/kotlin/com/budgetcalendar/di/ViewModelModule.kt
package com.budgetcalendar.di

import com.budgetcalendar.features.accounts.presentation.AccountListViewModel
import com.budgetcalendar.features.transactions.presentation.TransactionListViewModel
import com.budgetcalendar.features.calendar.presentation.CalendarViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AccountListViewModel(get(), get()) }
    viewModel { TransactionListViewModel(get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get()) }
}
```

### iOS Platform Module

```kotlin
// src/core/di/src/iosMain/kotlin/com/budgetcalendar/di/IosPlatformModule.kt
package com.budgetcalendar.di

import org.koin.dsl.module

val platformModule = module {
    // iOS-specific dependencies
}
```

## Component/Function Structure

### Application Setup (Android)

```kotlin
// androidMain/src/main/kotlin/com/budgetcalendar/BudgetCalendarApp.kt
package com.budgetcalendar

import android.app.Application
import com.budgetcalendar.di.AppModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BudgetCalendarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@BudgetCalendarApp)
            modules(AppModules.app)
        }
    }
}
```

### Application Setup (iOS)

```kotlin
// iosMain/src/main/kotlin/com/budgetcalendar/BudgetCalendarApp.kt
package com.budgetcalendar

import com.budgetcalendar.di.AppModules
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(AppModules.app)
    }
}
```

## Restrictions

- **ALWAYS** use constructor injection in ViewModels and UseCases
- **ALWAYS** define repositories with interfaces in domain layer, implement in data layer
- **ALWAYS** use `singleOf` with `bind` for interface/implementation pairs
- **ALWAYS** organize modules by layer (database, repository, useCase, viewModel)
- **NEVER** inject concrete implementations into UseCases - always use interfaces
- **ALWAYS** use `viewModel` scope for ViewModels in Android (with lifecycle awareness)
- **ALWAYS** declare `expect` classes for platform-specific dependencies
- **NEVER** use `new` to create dependencies inside classes - inject them instead
