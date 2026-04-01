---
name: viewmodel-kmp
description: ViewModel patterns for Jetpack Compose (Android) and SwiftUI (iOS)
---

# Skill: ViewModel KMP

## Project Context

- **Framework/Language**: Kotlin 1.9.22, Kotlin Multiplatform
- **Platforms**: 
  - Android: Jetpack Compose + Kotlin Coroutines + Flow
  - iOS: SwiftUI + Combine
- **Architecture**: MVVM
- **Project**: Budget Calendar

## Conventions

### Folder Structure

```
src/
├── features/
│   ├── accounts/
│   │   └── presentation/
│   │       ├── android/
│   │       │   └── viewmodel/
│   │       │       └── AccountListViewModel.kt
│   │       ├── ios/
│   │       │   └── viewmodel/
│   │       │       └── AccountListViewModel.kt
│   │       ├── android/
│   │       │   └── screen/
│   │       │       └── AccountListScreen.kt
│   │       └── ios/
│   │           └── screen/
│   │               └── AccountListView.swift
│   └── transactions/
│       └── ...
```

### Naming Conventions

- **ViewModels**: PascalCase, descriptive (e.g., `AccountListViewModel`, `CalendarViewModel`)
- **States**: PascalCase + `State` suffix (e.g., `AccountListState`, `TransactionListState`)
- **Events/Intents**: PascalCase + `Event` or `Intent` suffix
- **iOS Observables**: `@Published` properties, `ObservableObject` classes

### State Management Patterns

## Android (Jetpack Compose)

```kotlin
// src/features/accounts/presentation/android/viewmodel/AccountListViewModel.kt
package com.budgetcalendar.features.accounts.presentation.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.usecase.CreateAccountUseCase
import com.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI State
data class AccountListState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

// Events that can be triggered from UI
sealed class AccountListEvent {
    data object LoadAccounts : AccountListEvent()
    data object ShowAddDialog : AccountListEvent()
    data object HideAddDialog : AccountListEvent()
    data class CreateAccount(
        val name: String,
        val type: String,
        val balance: Long
    ) : AccountListEvent()
    data class DeleteAccount(val id: String) : AccountListEvent()
}

class AccountListViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AccountListState())
    val state: StateFlow<AccountListState> = _state.asStateFlow()

    init {
        handleEvent(AccountListEvent.LoadAccounts)
    }

    fun handleEvent(event: AccountListEvent) {
        when (event) {
            is AccountListEvent.LoadAccounts -> loadAccounts()
            is AccountListEvent.ShowAddDialog -> _state.update { it.copy(showAddDialog = true) }
            is AccountListEvent.HideAddDialog -> _state.update { it.copy(showAddDialog = false) }
            is AccountListEvent.CreateAccount -> createAccount(
                event.name,
                event.type,
                event.balance
            )
            is AccountListEvent.DeleteAccount -> deleteAccount(event.id)
        }
    }

    private fun loadAccounts() {
        getAccountsUseCase()
            .onEach { accounts ->
                _state.update {
                    it.copy(
                        accounts = accounts,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun createAccount(name: String, type: String, balance: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                createAccountUseCase(
                    name = name,
                    type = AccountType.valueOf(type),
                    balance = balance
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        showAddDialog = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create account"
                    )
                }
            }
        }
    }

    private fun deleteAccount(id: String) {
        viewModelScope.launch {
            try {
                // Implement delete use case
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Failed to delete account")
                }
            }
        }
    }
}
```

### Compose Screen Integration

```kotlin
// src/features/accounts/presentation/android/screen/AccountListScreen.kt
package com.budgetcalendar.features.accounts.presentation.android.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.budgetcalendar.features.accounts.presentation.android.viewmodel.AccountListEvent
import com.budgetcalendar.features.accounts.presentation.android.viewmodel.AccountListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountListScreen(
    viewModel: AccountListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { /* Top bar content */ },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.handleEvent(AccountListEvent.ShowAddDialog) }
            ) { /* Add icon */ }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> ErrorContent(state.error!!)
                else -> AccountListContent(
                    accounts = state.accounts,
                    onAccountClick = { /* Navigate to detail */ }
                )
            }
        }

        if (state.showAddDialog) {
            AddAccountDialog(
                onDismiss = { viewModel.handleEvent(AccountListEvent.HideAddDialog) },
                onConfirm = { name, type, balance ->
                    viewModel.handleEvent(
                        AccountListEvent.CreateAccount(name, type, balance)
                    )
                }
            )
        }
    }
}
```

## iOS (SwiftUI)

```swift
// src/features/accounts/presentation/ios/viewmodel/AccountListViewModel.swift
import Foundation
import Combine
import SwiftUI

// MARK: - Account Model
struct Account: Identifiable, Equatable {
    let id: String
    let name: String
    let type: AccountType
    let balance: Int64
    let colorHex: String?
    let isActive: Bool
    let createdAt: Int64
    let updatedAt: Int64
}

enum AccountType: String, CaseIterable {
    case checking = "CHECKING"
    case savings = "SAVINGS"
    case credit = "CREDIT"
    case cash = "CASH"
    case investment = "INVESTMENT"
}

// MARK: - View State
@MainActor
class AccountListViewModel: ObservableObject {
    @Published var accounts: [Account] = []
    @Published var isLoading: Bool = false
    @Published var error: String?
    @Published var showAddSheet: Bool = false
    
    private let getAccountsUseCase: GetAccountsUseCase
    private let createAccountUseCase: CreateAccountUseCase
    private var cancellables = Set<AnyCancellable>()
    
    init(
        getAccountsUseCase: GetAccountsUseCase,
        createAccountUseCase: CreateAccountUseCase
    ) {
        self.getAccountsUseCase = getAccountsUseCase
        self.createAccountUseCase = createAccountUseCase
        loadAccounts()
    }
    
    func loadAccounts() {
        isLoading = true
        error = nil
        
        getAccountsUseCase()
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let failure) = completion {
                        self?.error = failure.localizedDescription
                    }
                },
                receiveValue: { [weak self] accounts in
                    self?.accounts = accounts
                }
            )
            .store(in: &cancellables)
    }
    
    func createAccount(name: String, type: AccountType, balance: Int64) {
        Task {
            do {
                _ = try await createAccountUseCase(
                    name: name,
                    type: type,
                    balance: balance
                )
                showAddSheet = false
                loadAccounts()
            } catch {
                self.error = error.localizedDescription
            }
        }
    }
    
    func deleteAccount(id: String) {
        // Implement delete use case
    }
}
```

### SwiftUI View Integration

```swift
// src/features/accounts/presentation/ios/screen/AccountListView.swift
import SwiftUI

struct AccountListView: View {
    @StateObject private var viewModel: AccountListViewModel
    
    init(viewModel: AccountListViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                if viewModel.isLoading {
                    ProgressView()
                } else if let error = viewModel.error {
                    ErrorView(message: error) {
                        viewModel.loadAccounts()
                    }
                } else {
                    accountList
                }
            }
            .navigationTitle("Accounts")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        viewModel.showAddSheet = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $viewModel.showAddSheet) {
                AddAccountSheet(
                    onCancel: { viewModel.showAddSheet = false },
                    onSave: { name, type, balance in
                        viewModel.createAccount(name: name, type: type, balance: balance)
                    }
                )
            }
        }
    }
    
    private var accountList: some View {
        List(viewModel.accounts) { account in
            AccountRow(account: account)
        }
        .refreshable {
            viewModel.loadAccounts()
        }
    }
}

// MARK: - Supporting Views
struct AccountRow: View {
    let account: Account
    
    var body: some View {
        HStack {
            Circle()
                .fill(Color(hex: account.colorHex ?? "#007AFF"))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: account.iconName ?? "creditcard")
                        .foregroundColor(.white)
                )
            
            VStack(alignment: .leading) {
                Text(account.name)
                    .font(.headline)
                Text(account.type.rawValue)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Text(formatCurrency(account.balance))
                .font(.system(.body, design: .rounded))
        }
        .padding(.vertical, 4)
    }
    
    private func formatCurrency(_ amount: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "USD"
        return formatter.string(from: NSNumber(value: Double(amount) / 100)) ?? "$0.00"
    }
}

struct ErrorView: View {
    let message: String
    let onRetry: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundColor(.red)
            Text(message)
                .multilineTextAlignment(.center)
            Button("Retry", action: onRetry)
        }
        .padding()
    }
}

#Preview {
    // Preview implementation
}
```

### Shared Kotlin State for iOS (Kotlin Native)

```kotlin
// src/features/accounts/presentation/ios/AccountListState.kt
package com.budgetcalendar.features.accounts.presentation.ios

import com.budgetcalendar.features.accounts.domain.model.Account

data class AccountListState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSheet: Boolean = false
)

sealed class AccountListAction {
    data object LoadAccounts : AccountListAction()
    data object ShowAddSheet : AccountListAction()
    data object HideAddSheet : AccountListAction()
    data class CreateAccount(
        val name: String,
        val type: String,
        val balance: Long
    ) : AccountListAction()
    data class DeleteAccount(val id: String) : AccountListAction()
}
```

## Restrictions

- **ALWAYS** use `StateFlow` in Android ViewModels for UI state
- **ALWAYS** use `MutableStateFlow` for internal state updates
- **ALWAYS** expose immutable `StateFlow` to the UI layer
- **ALWAYS** use `@MainActor` or `@ObservedObject` for SwiftUI ViewModels
- **ALWAYS** use `Combine` publishers for iOS reactive streams
- **ALWAYS** handle errors gracefully with error states in UI
- **ALWAYS** use `koinViewModel()` in Compose for ViewModel injection
- **NEVER** expose raw database queries or entities to the UI layer
- **ALWAYS** define clear state, events/intents, and effects pattern
- **ALWAYS** use sealed classes for events/intents to ensure exhaustive handling
