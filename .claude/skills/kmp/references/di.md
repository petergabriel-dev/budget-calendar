# KMP Reference: Koin Dependency Injection

## Core Principle

Constructor injection always. Never call `get()` inside a class body. Dependencies arrive through the constructor — the class never knows about the DI container.

```kotlin
// CORRECT
class CreateAccountUseCase(private val repository: IAccountRepository)

// WRONG — class body knows about Koin
class CreateAccountUseCase {
    private val repository: IAccountRepository by inject()  // never do this
}
```

---

## Scope Rules

| Scope | Koin DSL | Use For |
|-------|----------|---------|
| Singleton | `single { }` | Repositories, Database, shared stateful services |
| Factory | `factory { }` | Use cases (stateless, cheap to create) |
| ViewModel | `viewModel { }` | ViewModels (scoped to Compose/Fragment lifecycle) |

**Captive dependency warning**: Never inject a `factory`-scoped component into a `single`. The singleton will hold the factory's first instance forever, violating its intended transient lifecycle.

```kotlin
// WRONG — captive dependency
single { MyService(get<TransientHelper>()) }  // TransientHelper is factory-scoped

// CORRECT — create scope dynamically if needed, or make TransientHelper a single too
```

---

## Interface Rule (Package-by-Feature)

Create an interface when **layer isolation requires it** (repository: always) or **multiple implementations exist**.
Do NOT create an interface for use cases — they have one implementation (YAGNI).

| Component | Interface? | Reason |
|-----------|-----------|--------|
| Repository | **Yes** — always | Domain layer must not depend on SQLDelight |
| Use Case | No | Single impl, no layer boundary crossed |
| ViewModel | No | Single impl, no layer boundary |
| Database drivers | Yes (via Koin binding) | Platform-specific impl |

---

## Module Structure — Package-by-Feature

Each feature exports its own Koin module. The root `KoinModules.kt` in `core/di/` assembles them all.

### Feature Module Example

```kotlin
// features/accounts/di/AccountModule.kt  (or at bottom of any file in the feature)
val accountModule = module {

    // Data layer — singleton (SQLDelight queries are stateless wrappers)
    single<IAccountRepository> {
        AccountRepositoryImpl(
            queries = get<BudgetCalendarDatabase>().accountQueries,
            mapper = AccountMapper()
        )
    }

    // Domain layer — factory (use cases are stateless)
    factory { GetAccountsUseCase(repository = get()) }
    factory { CreateAccountUseCase(repository = get()) }
    factory { UpdateAccountUseCase(repository = get()) }
    factory { DeleteAccountUseCase(repository = get()) }
    factory { GetSpendingPoolAccountsUseCase(repository = get()) }

    // Presentation layer — viewModel scope
    viewModel {
        AccountViewModel(
            getAccounts = get(),
            createAccount = get(),
            updateAccount = get(),
            deleteAccount = get()
        )
    }
}
```

### Root Assembly

```kotlin
// core/di/KoinModules.kt
val databaseModule = module {
    single { createBudgetCalendarDatabase(driver = get()) }
    // driver is provided by platform modules (androidModule / iosModule)
}

val allModules = listOf(
    databaseModule,
    accountModule,
    transactionModule,
    calendarModule,
    budgetModule,
    sandboxModule
)
```

### Platform Modules

```kotlin
// androidMain: core/di/AndroidModule.kt
val androidModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            context = get(),        // Android Context — only available here
            name = "budget_calendar.db"
        )
    }
}

// iosMain: core/di/IosModule.kt
val iosModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            name = "budget_calendar.db"
        )
    }
}
```

---

## Koin Initialization

### Android (`androidMain`)

```kotlin
// androidMain/MainActivity.kt  (or Application class)
class BudgetCalendarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BudgetCalendarApp)
            modules(androidModule + allModules)
        }
    }
}
```

### iOS (`iosMain`)

```kotlin
// iosMain/MainViewController.kt
fun initKoin() {
    startKoin {
        modules(iosModule + allModules)
    }
}
```

---

## ViewModel Injection in Compose

```kotlin
// In any Composable (commonMain)
@Composable
fun AccountScreen() {
    val viewModel: AccountViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

Use `koinViewModel()` from `koin-compose`. Never instantiate ViewModels directly.

---

## Anti-Patterns to Avoid

| Anti-Pattern | Why | Fix |
|-------------|-----|-----|
| `by inject()` in class body | Couples class to Koin | Constructor inject |
| `KoinComponent` on domain/data classes | Leaks DI framework into business logic | Constructor inject |
| One-interface-per-class | "Compliance theatre", pollutes codebase | Only interface where layer isolation needed |
| `single` for use cases | Use cases are stateless; single wastes nothing but signals wrong intent | `factory` |
| `get()` chains in module | Hard to test | Name parameters clearly: `repository = get()` |
