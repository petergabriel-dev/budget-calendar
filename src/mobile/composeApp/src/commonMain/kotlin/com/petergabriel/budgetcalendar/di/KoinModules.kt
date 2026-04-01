package com.petergabriel.budgetcalendar.di

import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.features.accounts.data.mapper.AccountMapper
import com.petergabriel.budgetcalendar.features.accounts.data.repository.AccountRepositoryImpl
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CalculateNetWorthUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CreateAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.DeleteAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountByIdUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetSpendingPoolAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.UpdateAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.presentation.AccountViewModel
import com.petergabriel.budgetcalendar.features.budget.data.mapper.BudgetMapper
import com.petergabriel.budgetcalendar.features.budget.data.repository.BudgetRepositoryImpl
import com.petergabriel.budgetcalendar.features.budget.data.repository.MonthlyRolloverRepositoryImpl
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IMonthlyRolloverRepository
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateMonthEndRolloverUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.CalculateSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.GetCreditCardReservationsUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.GetRolloverHistoryUseCase
import com.petergabriel.budgetcalendar.features.budget.domain.usecase.SaveMonthlyRolloverUseCase
import com.petergabriel.budgetcalendar.features.budget.presentation.BudgetViewModel
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.BuildCalendarMonthUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.CalculateMonthProjectionUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.CalculateDaySummaryUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.GetMonthTransactionsUseCase
import com.petergabriel.budgetcalendar.features.calendar.presentation.CalendarViewModel
import com.petergabriel.budgetcalendar.features.creditcard.data.mapper.CreditCardMapper
import com.petergabriel.budgetcalendar.features.creditcard.data.repository.CreditCardRepositoryImpl
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.CalculateReservedAmountUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.CreateCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummaryByIdUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.MakeCreditCardPaymentUseCase
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.UpdateCreditCardSettingsUseCase
import com.petergabriel.budgetcalendar.features.creditcard.presentation.CreditCardViewModel
import com.petergabriel.budgetcalendar.features.recurring.data.mapper.RecurringTransactionMapper
import com.petergabriel.budgetcalendar.features.recurring.data.repository.RecurringTransactionRepositoryImpl
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.CreateRecurringTransactionUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.DeleteRecurringTransactionUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.GenerateMonthlyTransactionsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.GetActiveRecurringTransactionsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.GetRecurringTransactionsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.GetUpcomingGeneratedTransactionsUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.ToggleRecurringActiveUseCase
import com.petergabriel.budgetcalendar.features.recurring.domain.usecase.UpdateRecurringTransactionUseCase
import com.petergabriel.budgetcalendar.features.recurring.presentation.RecurringViewModel
import com.petergabriel.budgetcalendar.features.sandbox.data.mapper.SandboxMapper
import com.petergabriel.budgetcalendar.features.sandbox.data.repository.SandboxRepositoryImpl
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.AddSimulationTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CheckAndExpireSandboxesUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CompareSandboxWithRealityUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.CreateSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.DeleteSandboxUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxByIdUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxSafeToSpendUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxTransactionsUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.GetSandboxesUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.PromoteTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.RemoveSimulationTransactionUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.RunSimulationUseCase
import com.petergabriel.budgetcalendar.features.sandbox.domain.usecase.UpdateSnapshotLastAccessedUseCase
import com.petergabriel.budgetcalendar.features.sandbox.presentation.SandboxViewModel
import com.petergabriel.budgetcalendar.features.transactions.data.mapper.TransactionMapper
import com.petergabriel.budgetcalendar.features.transactions.data.repository.TransactionRepositoryImpl
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.DeleteTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetPendingAndOverdueExpensesByAccountUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetPendingTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.GetTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.MarkOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.UpdateTransactionStatusUseCase
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val databaseModule = module {
    single { BudgetCalendarDatabase(get()) }
    single { AccountMapper() }
    single { TransactionMapper() }
    single { RecurringTransactionMapper() }
    single { BudgetMapper() }
    single { SandboxMapper() }
    single { CreditCardMapper() }
}

val repositoryModule = module {
    single { AccountRepositoryImpl(get(), get()) } bind IAccountRepository::class
    single { TransactionRepositoryImpl(get(), get()) } bind ITransactionRepository::class
    single { BudgetRepositoryImpl(get(), get()) } bind IBudgetRepository::class
    single { MonthlyRolloverRepositoryImpl(get(), get()) } bind IMonthlyRolloverRepository::class
    single { RecurringTransactionRepositoryImpl(get(), get()) } bind IRecurringTransactionRepository::class
    single { SandboxRepositoryImpl(get(), get()) } bind ISandboxRepository::class
    single { CreditCardRepositoryImpl(get(), get()) } bind ICreditCardRepository::class
}

val useCaseModule = module {
    factory { CreateAccountUseCase(get(), get()) }
    factory { GetAccountsUseCase(get()) }
    factory { GetAccountByIdUseCase(get()) }
    factory { UpdateAccountUseCase(get()) }
    factory { DeleteAccountUseCase(get()) }
    factory { GetSpendingPoolAccountsUseCase(get()) }
    factory { CalculateNetWorthUseCase(get()) }

    factory { CreateTransactionUseCase(get()) }
    factory { GetTransactionsUseCase(get()) }
    factory { GetPendingTransactionsUseCase(get()) }
    factory { GetOverdueTransactionsUseCase(get()) }
    factory { UpdateTransactionStatusUseCase(get(), get()) }
    factory { DeleteTransactionUseCase(get()) }
    factory { MarkOverdueTransactionsUseCase(get()) }
    factory { GetPendingAndOverdueExpensesByAccountUseCase(get()) }

    factory { CalculateSafeToSpendUseCase(get(), get(), get()) }
    factory { GetCreditCardReservationsUseCase(get()) }
    factory { SaveMonthlyRolloverUseCase(get()) }
    factory { GetRolloverHistoryUseCase(get()) }
    factory { CalculateMonthEndRolloverUseCase(get(), get()) }

    factory { GetMonthTransactionsUseCase(get()) }
    factory { CalculateDaySummaryUseCase() }
    factory { BuildCalendarMonthUseCase(get()) }
    factory { CalculateMonthProjectionUseCase(get(), get()) }

    factory { CreateRecurringTransactionUseCase(get()) }
    factory { GetRecurringTransactionsUseCase(get()) }
    factory { GetActiveRecurringTransactionsUseCase(get()) }
    factory { UpdateRecurringTransactionUseCase(get()) }
    factory { GenerateMonthlyTransactionsUseCase(get(), get()) }
    factory { ToggleRecurringActiveUseCase(get(), get()) }
    factory { DeleteRecurringTransactionUseCase(get()) }
    factory { GetUpcomingGeneratedTransactionsUseCase(get(), get()) }

    factory { CreateSandboxUseCase(get(), get()) }
    factory { GetSandboxesUseCase(get()) }
    factory { GetSandboxByIdUseCase(get()) }
    factory { GetSandboxTransactionsUseCase(get()) }
    factory { AddSimulationTransactionUseCase(get(), get()) }
    factory { RemoveSimulationTransactionUseCase(get()) }
    factory { GetSandboxSafeToSpendUseCase(get()) }
    factory { CompareSandboxWithRealityUseCase(get(), get(), get()) }
    factory { PromoteTransactionUseCase(get(), get()) }
    factory { DeleteSandboxUseCase(get()) }
    factory { CheckAndExpireSandboxesUseCase(get()) }
    factory { RunSimulationUseCase() }
    factory { UpdateSnapshotLastAccessedUseCase(get()) }

    factory { CreateCreditCardSettingsUseCase(get(), get()) }
    factory { GetCreditCardSettingsUseCase(get(), get()) }
    factory { UpdateCreditCardSettingsUseCase(get()) }
    factory { CalculateReservedAmountUseCase(get()) }
    factory { GetCreditCardSummariesUseCase(get(), get(), get()) }
    factory { GetCreditCardSummaryByIdUseCase(get(), get(), get()) }
    factory { MakeCreditCardPaymentUseCase(get(), get()) }
}

val viewModelModule = module {
    viewModel { AccountViewModel(get(), get(), get(), get(), get()) }
    viewModel { TransactionViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TransactionFormViewModel(get(), get(), get(), get()) }
    viewModel { BudgetViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get(), get()) }
    viewModel { RecurringViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SandboxViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CreditCardViewModel(get(), get(), get(), get(), get(), get(), get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    return startKoin {
        appDeclaration()
        modules(
            platformModule,
            databaseModule,
            repositoryModule,
            useCaseModule,
            viewModelModule,
        )
    }
}
