# KMP Reference: Clean Architecture + MVVM

## Layer Import Rules

Strict dependency direction — each layer only imports from the layer below it:

```
Presentation → Domain ← Data
                ↑
            (Domain defines interfaces; Data implements them)
```

| Layer | Package | Can Import | Cannot Import |
|-------|---------|-----------|---------------|
| Presentation | `features/x/presentation/` | Domain models, Use cases, UiState | Data layer, SQLDelight, Koin internals |
| Domain | `features/x/domain/` | Domain models only | Presentation, Data, SQLDelight, Koin, Android SDK |
| Data | `features/x/data/` | Domain interfaces + models, SQLDelight | Presentation, ViewModels |
| Core | `core/` | SQLDelight runtime, Koin | Feature packages |

The Domain layer is pure Kotlin. It has zero platform or framework imports.

---

## UiState Pattern

Each screen has one immutable state class. ViewModel emits it as a `StateFlow`.

```kotlin
// features/accounts/presentation/AccountUiState.kt
data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalNetWorth: Long = 0L   // cents
)
```

Rules:
- All fields have defaults — UI always has a valid initial state
- Never use sealed class for UiState unless the screen has fundamentally distinct modes (rare)
- Loading and error are fields on the state, not separate flows

---

## Intent / Event Pattern

User actions are expressed as sealed classes dispatched to the ViewModel.

```kotlin
// features/accounts/presentation/AccountUiState.kt (or separate file)
sealed interface AccountIntent {
    data class CreateAccount(val request: CreateAccountRequest) : AccountIntent
    data class DeleteAccount(val id: Long) : AccountIntent
    data class SelectAccount(val id: Long) : AccountIntent
    object LoadAccounts : AccountIntent
}
```

Rules:
- Intents are **side-effect triggers**, not state setters
- ViewModel processes one intent at a time via `onIntent(intent: AccountIntent)`
- Navigation side-effects use `SharedFlow<XxxEffect>` (one-shot events), separate from `uiState`

---

## ViewModel Pattern

```kotlin
// features/accounts/presentation/AccountViewModel.kt
class AccountViewModel(
    private val getAccounts: GetAccountsUseCase,
    private val createAccount: CreateAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AccountEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AccountEffect> = _effects.asSharedFlow()

    init { onIntent(AccountIntent.LoadAccounts) }

    fun onIntent(intent: AccountIntent) {
        viewModelScope.launch {
            when (intent) {
                is AccountIntent.LoadAccounts -> loadAccounts()
                is AccountIntent.CreateAccount -> createAccount(intent.request)
                is AccountIntent.DeleteAccount -> deleteAccount(intent.id)
                is AccountIntent.SelectAccount -> selectAccount(intent.id)
            }
        }
    }

    private suspend fun loadAccounts() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        getAccounts()
            .onSuccess { accounts ->
                _uiState.update { it.copy(accounts = accounts, isLoading = false) }
            }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
    }
}
```

Rules:
- ViewModel constructor receives only use case classes — no repositories directly
- All async work runs inside `viewModelScope.launch`
- State updates use `.update { it.copy(...) }` — never replace the whole state object

---

## Use Case Pattern

One class, one responsibility. Use `operator fun invoke` for clean call-site syntax.

```kotlin
// features/accounts/domain/usecase/GetAccountsUseCase.kt
class GetAccountsUseCase(
    private val repository: IAccountRepository
) {
    suspend operator fun invoke(): Result<List<Account>> =
        repository.getAll()
}
```

For reactive use cases returning a stream:

```kotlin
class ObserveAccountsUseCase(
    private val repository: IAccountRepository
) {
    operator fun invoke(): Flow<List<Account>> =
        repository.observeAll()
}
```

Rules:
- Use cases are **not** interfaces — only one implementation ever exists (YAGNI)
- Single public method: `invoke()` (operator fun)
- Suspend for one-shot operations, `Flow` for reactive streams
- Never contain UI logic or ViewModel references

---

## Repository Interface Pattern

Interface lives in `domain/repository/`, implementation in `data/repository/`.

```kotlin
// features/accounts/domain/repository/IAccountRepository.kt
interface IAccountRepository {
    suspend fun getAll(): Result<List<Account>>
    fun observeAll(): Flow<List<Account>>
    suspend fun getById(id: Long): Result<Account>
    suspend fun insert(request: CreateAccountRequest): Result<Account>
    suspend fun update(id: Long, request: UpdateAccountRequest): Result<Account>
    suspend fun delete(id: Long): Result<Unit>
    fun observeSpendingPool(): Flow<List<Account>>
}
```

Implementation:

```kotlin
// features/accounts/data/repository/AccountRepositoryImpl.kt
class AccountRepositoryImpl(
    private val queries: AccountQueries,   // SQLDelight generated
    private val mapper: AccountMapper
) : IAccountRepository {

    override suspend fun getAll(): Result<List<Account>> = runCatching {
        queries.getAllAccounts().executeAsList().map { mapper.toDomain(it) }
    }

    override fun observeAll(): Flow<List<Account>> =
        queries.getAllAccounts().asFlow().mapToList(Dispatchers.IO).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    // ...
}
```

---

## expect/actual — Decision Matrix

| Scenario | Use |
|----------|-----|
| Stateless utility (UUID, platform name, current time) | `expect fun` / `actual fun` |
| Database driver | Interface + Koin (inject `SqlDriver`) |
| File system access | Interface + Koin |
| Anything with lifecycle or state | Interface + Koin |
| Anything requiring Android `Context` | Interface + Koin (never in commonMain) |

**Warning**: `expect class` is experimental and creates brittle coupling. Prefer `expect fun` for utilities and interfaces+DI for everything else.
