package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.accounts.data.mapper.AccountMapper
import com.petergabriel.budgetcalendar.features.accounts.data.repository.AccountRepositoryImpl
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CreateAccountUseCase
import com.petergabriel.budgetcalendar.features.budget.data.mapper.BudgetMapper
import com.petergabriel.budgetcalendar.features.budget.data.repository.BudgetRepositoryImpl
import com.petergabriel.budgetcalendar.features.budget.data.repository.MonthlyRolloverRepositoryImpl
import com.petergabriel.budgetcalendar.features.creditcard.data.mapper.CreditCardMapper
import com.petergabriel.budgetcalendar.features.creditcard.data.repository.CreditCardRepositoryImpl
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.domain.usecase.GetCreditCardSummariesUseCase
import com.petergabriel.budgetcalendar.features.notifications.data.mapper.NotificationMapper
import com.petergabriel.budgetcalendar.features.notifications.data.repository.NotificationRepositoryImpl
import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GenerateNotificationsUseCase
import com.petergabriel.budgetcalendar.features.transactions.data.mapper.TransactionMapper
import com.petergabriel.budgetcalendar.features.transactions.data.repository.TransactionRepositoryImpl
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.UpdateTransactionStatusRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.CreateTransactionUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.MarkOverdueTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.usecase.UpdateTransactionStatusUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetFlowObservationIntegrationTest {

    @Test
    fun calculateNetWorth_includesPendingCardChargeAsSignedProjectedDebt() = runBlocking {
        val fixture = createFixture()

        try {
            val checking = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 100_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()
            val card = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = 0L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 30_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Pending card charge",
                ),
            ).getOrThrow()
            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = checking.id,
                    amount = 20_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Pending pool expense",
                ),
            ).getOrThrow()

            assertEquals(70_00L, fixture.accountRepository.calculateNetWorth())
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun calculateNetWorth_treatsOverpaidCardAsPositiveCredit() = runBlocking {
        val fixture = createFixture()

        try {
            val checking = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 100_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()
            val card = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = -30_00L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = checking.id,
                    destinationAccountId = card.id,
                    amount = 40_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.TRANSFER,
                    status = TransactionStatus.CONFIRMED,
                    description = "Overpay card",
                ),
            ).getOrThrow()

            assertEquals(70_00L, fixture.accountRepository.calculateNetWorth())
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun getCreditCardSummaries_reemitsProjectedPendingChargeOnTransactionWrite() = runBlocking {
        val fixture = createFixture()

        try {
            val card = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = 0L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            fixture.creditCardRepository.insert(
                accountId = card.id,
                creditLimit = 100_00L,
                statementBalance = null,
                dueDate = null,
            ).getOrThrow()
            val useCase = GetCreditCardSummariesUseCase(
                creditCardRepository = fixture.creditCardRepository,
                accountRepository = fixture.accountRepository,
                transactionRepository = fixture.transactionRepository,
            )
            val balances = mutableListOf<Long>()
            var finalSummary: CreditCardSummary? = null
            val collectJob = launch {
                withTimeout(5_000) {
                    useCase().first { summaries ->
                        val summary = summaries.singleOrNull() ?: return@first false
                        balances += summary.currentBalance
                        if (summary.currentBalance == -30_00L) {
                            finalSummary = summary
                            true
                        } else {
                            false
                        }
                    }
                }
            }

            withTimeout(5_000) {
                while (balances.isEmpty()) {
                    delay(10)
                }
            }
            assertEquals(0L, balances.first())

            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 30_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Pending card charge",
                ),
            ).getOrThrow()
            collectJob.join()

            val summary = requireNotNull(finalSummary)
            assertEquals(-30_00L, summary.currentBalance)
            assertEquals(30_00L, summary.reservedAmount)
            assertEquals(70_00L, summary.availableCredit)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun calculateSafeToSpend_reemitsAfterCreatingPendingExpense() = runBlocking {
        val fixture = createFixture()

        try {
            val createAccountResult = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            )
            assertTrue(createAccountResult.isSuccess)
            val account = createAccountResult.getOrThrow()

            val emissions = mutableListOf<com.petergabriel.budgetcalendar.features.budget.domain.model.BudgetSummary>()
            val collectJob = launch {
                withTimeout(5_000) {
                    fixture.calculateSafeToSpendUseCase()
                        .first { summary ->
                            emissions += summary
                            summary.pendingReservations == 75_00L
                        }
                }
            }

            withTimeout(5_000) {
                while (emissions.isEmpty()) {
                    delay(10)
                }
            }

            val initial = emissions.first()
            assertEquals(500_00L, initial.availableToSpend)
            assertEquals(0L, initial.pendingReservations)

            val createTransactionResult = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 75_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Groceries",
                    category = "Food",
                ),
            )
            assertTrue(createTransactionResult.isSuccess)

            collectJob.join()

            val updated = emissions.last { summary -> summary.pendingReservations == 75_00L }
            assertEquals(75_00L, updated.pendingReservations)
            assertEquals(425_00L, updated.availableToSpend)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun createConfirmedExpense_deductsAccountBalance() = runBlocking {
        val fixture = createFixture()

        try {
            val account = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()

            val created = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 75_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.CONFIRMED,
                    description = "Confirmed expense",
                    category = "Food",
                ),
            )
            assertTrue(created.isSuccess)
            assertEquals(TransactionStatus.CONFIRMED, created.getOrThrow().status)

            val summary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { sts ->
                    sts.pendingReservations == 0L &&
                        sts.availableToSpend == 425_00L &&
                        sts.confirmedSpending == 75_00L
                }
            }

            assertEquals(425_00L, summary.availableToSpend)
            assertEquals(75_00L, summary.confirmedSpending)
            assertEquals(425_00L, fixture.accountRepository.getAccountById(account.id)?.balance)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun confirmingPendingExpense_updatesAccountBalance_andKeepsSafeToSpendStable() = runBlocking {
        val fixture = createFixture()

        try {
            val account = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()

            val pendingExpense = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 75_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Pending expense",
                ),
            ).getOrThrow()

            val pendingSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary ->
                    summary.pendingReservations == 75_00L
                }
            }
            assertEquals(425_00L, pendingSummary.availableToSpend)

            val confirmResult = fixture.updateTransactionStatusUseCase(
                pendingExpense.id,
                UpdateTransactionStatusRequest(status = TransactionStatus.CONFIRMED),
            )
            assertTrue(confirmResult.isSuccess)

            val confirmedSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary ->
                    summary.pendingReservations == 0L && summary.availableToSpend == 425_00L
                }
            }

            assertEquals(425_00L, confirmedSummary.availableToSpend)
            assertEquals(75_00L, confirmedSummary.confirmedSpending)
            assertEquals(425_00L, fixture.accountRepository.getAccountById(account.id)?.balance)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun overdueNotification_reactiveUpdate_updatesCountWithoutDuplicate() = runBlocking {
        val fixture = createFixture()

        try {
            val account = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()

            val overdueDate = DateUtils.startOfDayMillis(DateUtils.nowMillis()) - (2L * DateUtils.MILLIS_IN_DAY)

            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 50_00L,
                    date = overdueDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Overdue A",
                ),
            ).getOrThrow()

            fixture.markOverdueTransactionsUseCase(DateUtils.nowMillis())
            fixture.generateNotificationsUseCase.checkOverdueNotification(
                overdueTransactions = fixture.transactionRepository.getOverdueTransactions().first(),
                nowMillis = DateUtils.nowMillis(),
            )

            val firstOverdueNotification = fixture.notificationRepository
                .getActiveNotifications()
                .first()
                .first { notification -> notification.type == NotificationType.OVERDUE_TRANSACTIONS }
            assertEquals(1, firstOverdueNotification.overdueCount)

            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = account.id,
                    amount = 75_00L,
                    date = overdueDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Overdue B",
                ),
            ).getOrThrow()

            fixture.markOverdueTransactionsUseCase(DateUtils.nowMillis())
            fixture.generateNotificationsUseCase.checkOverdueNotification(
                overdueTransactions = fixture.transactionRepository.getOverdueTransactions().first(),
                nowMillis = DateUtils.nowMillis(),
            )

            val overdueNotifications = fixture.notificationRepository
                .getActiveNotifications()
                .first()
                .filter { notification -> notification.type == NotificationType.OVERDUE_TRANSACTIONS }

            assertEquals(1, overdueNotifications.size)
            assertEquals(2, overdueNotifications.first().overdueCount)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun migration11_confirmsExistingOverdueCreditCardChargesOnly() = runBlocking {
        val fixture = createFixture()

        try {
            val checking = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()
            val card = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card A",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = 0L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            val transactionDate = DateUtils.startOfDayMillis(DateUtils.nowMillis())
            val overdueCard = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 75_00L,
                    date = transactionDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.OVERDUE,
                    description = "Overdue card",
                ),
            ).getOrThrow()
            val pendingCard = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 25_00L,
                    date = transactionDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Pending card",
                ),
            ).getOrThrow()
            val cancelledCard = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 10_00L,
                    date = transactionDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.CANCELLED,
                    description = "Cancelled card",
                ),
            ).getOrThrow()
            val overdueChecking = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = checking.id,
                    amount = 50_00L,
                    date = transactionDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.OVERDUE,
                    description = "Overdue checking",
                ),
            ).getOrThrow()

            BudgetCalendarDatabase.Schema.migrate(fixture.driver, oldVersion = 11, newVersion = 12)

            assertEquals(
                TransactionStatus.CONFIRMED,
                fixture.transactionRepository.getTransactionById(overdueCard.id)?.status,
            )
            assertEquals(
                TransactionStatus.PENDING,
                fixture.transactionRepository.getTransactionById(pendingCard.id)?.status,
            )
            assertEquals(
                TransactionStatus.CANCELLED,
                fixture.transactionRepository.getTransactionById(cancelledCard.id)?.status,
            )
            assertEquals(
                TransactionStatus.OVERDUE,
                fixture.transactionRepository.getTransactionById(overdueChecking.id)?.status,
            )
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun markOverdueTransactions_excludesCreditCardChargesFromOverdueLifecycle() = runBlocking {
        val fixture = createFixture()

        try {
            val checking = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 500_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()
            val card = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card A",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = 0L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            val overdueDate = DateUtils.startOfDayMillis(DateUtils.nowMillis()) - (2L * DateUtils.MILLIS_IN_DAY)

            val checkingCharge = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = checking.id,
                    amount = 50_00L,
                    date = overdueDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Checking overdue",
                ),
            ).getOrThrow()
            val cardCharge = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 75_00L,
                    date = overdueDate,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Card stays pending",
                ),
            ).getOrThrow()

            val markedCount = fixture.markOverdueTransactionsUseCase(DateUtils.nowMillis())
            assertEquals(1, markedCount)
            assertEquals(
                TransactionStatus.OVERDUE,
                fixture.transactionRepository.getTransactionById(checkingCharge.id)?.status,
            )
            assertEquals(
                TransactionStatus.PENDING,
                fixture.transactionRepository.getTransactionById(cardCharge.id)?.status,
            )

            val pendingTransactions = fixture.transactionRepository.getPendingTransactions().first()
            assertEquals(listOf(cardCharge.id), pendingTransactions.map { transaction -> transaction.id })

            fixture.generateNotificationsUseCase.checkOverdueNotification(
                overdueTransactions = fixture.transactionRepository.getOverdueTransactions().first(),
                nowMillis = DateUtils.nowMillis(),
            )
            val overdueNotification = fixture.notificationRepository
                .getActiveNotifications()
                .first()
                .single { notification -> notification.type == NotificationType.OVERDUE_TRANSACTIONS }
            assertEquals(1, overdueNotification.overdueCount)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun creditCardCycleSettings_doNotChangeSafeToSpendOrNetWorth() = runBlocking {
        val fixture = createFixture()

        try {
            fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 1_000_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()
            val card = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = 0L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            fixture.creditCardRepository.insert(
                accountId = card.id,
                creditLimit = 500_00L,
                statementBalance = null,
                dueDate = null,
                statementClosingDay = null,
                paymentDueDay = null,
            ).getOrThrow()
            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 120_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Card charge",
                ),
            ).getOrThrow()

            val beforeSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary -> summary.creditCardReserved == 120_00L }
            }
            val beforeNetWorth = fixture.accountRepository.calculateNetWorth()

            fixture.creditCardRepository.update(
                accountId = card.id,
                creditLimit = 500_00L,
                statementBalance = null,
                dueDate = null,
                statementClosingDay = 28,
                paymentDueDay = 15,
            ).getOrThrow()

            val afterSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary -> summary.creditCardReserved == 120_00L }
            }
            val afterNetWorth = fixture.accountRepository.calculateNetWorth()

            assertEquals(beforeSummary.availableToSpend, afterSummary.availableToSpend)
            assertEquals(beforeSummary.creditCardReserved, afterSummary.creditCardReserved)
            assertEquals(beforeSummary.pendingReservations, afterSummary.pendingReservations)
            assertEquals(beforeNetWorth, afterNetWorth)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun creditCardReserve_tracksProjectedUnpaidBalanceAcrossLifecycle() = runBlocking {
        val fixture = createFixture()

        try {
            val checking = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Primary Checking",
                    type = AccountType.CHECKING,
                    initialBalance = 1_000_00L,
                    isInSpendingPool = true,
                ),
            ).getOrThrow()
            val card = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card A",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = 0L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()

            val charge = fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 120_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Card charge",
                ),
            ).getOrThrow()

            val pendingSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary ->
                    summary.creditCardReserved == 120_00L && summary.availableToSpend == 880_00L
                }
            }
            assertEquals(120_00L, pendingSummary.creditCardReserved)
            assertEquals(120_00L, fixture.database.budgetQueries.getCreditCardReservedAmount().executeAsOne())

            val confirmResult = fixture.updateTransactionStatusUseCase(
                charge.id,
                UpdateTransactionStatusRequest(status = TransactionStatus.CONFIRMED),
            )
            assertTrue(confirmResult.isSuccess)

            val confirmedSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary ->
                    summary.creditCardReserved == 120_00L && summary.availableToSpend == 880_00L
                }
            }
            assertEquals(120_00L, confirmedSummary.creditCardReserved)
            assertEquals(-120_00L, fixture.accountRepository.getAccountById(card.id)?.balance)

            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = checking.id,
                    destinationAccountId = card.id,
                    amount = 120_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.TRANSFER,
                    status = TransactionStatus.CONFIRMED,
                    description = "Card payment",
                ),
            ).getOrThrow()

            val paidSummary = withTimeout(5_000) {
                fixture.calculateSafeToSpendUseCase().first { summary ->
                    summary.creditCardReserved == 0L && summary.availableToSpend == 880_00L
                }
            }
            assertEquals(0L, paidSummary.creditCardReserved)
            assertEquals(0L, fixture.accountRepository.getAccountById(card.id)?.balance)

            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = card.id,
                    amount = 999_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.CANCELLED,
                    description = "Cancelled card charge",
                ),
            ).getOrThrow()
            val cardWithDebt = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card B",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = 0L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = cardWithDebt.id,
                    amount = 50_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PENDING,
                    description = "Second card charge",
                ),
            ).getOrThrow()
            val overpaidCard = fixture.createAccountUseCase(
                CreateAccountRequest(
                    name = "Card C",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = -30_00L,
                    isInSpendingPool = false,
                ),
            ).getOrThrow()
            fixture.createTransactionUseCase(
                CreateTransactionRequest(
                    accountId = checking.id,
                    destinationAccountId = overpaidCard.id,
                    amount = 40_00L,
                    date = DateUtils.startOfDayMillis(DateUtils.nowMillis()),
                    type = TransactionType.TRANSFER,
                    status = TransactionStatus.CONFIRMED,
                    description = "Overpay card",
                ),
            ).getOrThrow()

            val reservations = fixture.database.budgetQueries
                .getCreditCardReservations()
                .executeAsList()
                .associate { reservation -> reservation.account_id to reservation.reserved_amount }
            assertEquals(0L, reservations[card.id])
            assertEquals(50_00L, reservations[cardWithDebt.id])
            assertEquals(0L, reservations[overpaidCard.id])
            assertEquals(50_00L, fixture.database.budgetQueries.getCreditCardReservedAmount().executeAsOne())
            assertEquals(
                -50_00L,
                fixture.database.credit_card_settingsQueries
                    .getCreditCardProjectedBalance(cardWithDebt.id)
                    .executeAsOne(),
            )

            val projectedBalances = fixture.database.credit_card_settingsQueries
                .getAllCreditCardProjectedBalances { accountId, projectedBalance -> accountId to projectedBalance }
                .executeAsList()
                .associate { row -> row.first to row.second }
            assertEquals(0L, projectedBalances[card.id])
            assertEquals(-50_00L, projectedBalances[cardWithDebt.id])
            assertEquals(10_00L, projectedBalances[overpaidCard.id])
        } finally {
            fixture.driver.close()
        }
    }

    private fun createFixture(): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetCalendarDatabase.Schema.create(driver)
        val database = BudgetCalendarDatabase(driver)

        val transactionRepository = TransactionRepositoryImpl(database, TransactionMapper())
        val creditCardRepository = CreditCardRepositoryImpl(database, CreditCardMapper())
        val accountRepository = AccountRepositoryImpl(database, AccountMapper(), transactionRepository, creditCardRepository)
        val budgetMapper = BudgetMapper()
        val budgetRepository = BudgetRepositoryImpl(database, budgetMapper, transactionRepository)
        val monthlyRolloverRepository = MonthlyRolloverRepositoryImpl(database, budgetMapper)
        val notificationRepository = NotificationRepositoryImpl(database, NotificationMapper())

        return Fixture(
            driver = driver,
            database = database,
            transactionRepository = transactionRepository,
            creditCardRepository = creditCardRepository,
            notificationRepository = notificationRepository,
            accountRepository = accountRepository,
            createAccountUseCase = CreateAccountUseCase(accountRepository),
            createTransactionUseCase = CreateTransactionUseCase(transactionRepository),
            updateTransactionStatusUseCase = UpdateTransactionStatusUseCase(transactionRepository),
            markOverdueTransactionsUseCase = MarkOverdueTransactionsUseCase(transactionRepository),
            generateNotificationsUseCase = GenerateNotificationsUseCase(notificationRepository),
            calculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(
                budgetRepository,
                monthlyRolloverRepository,
                transactionRepository,
            ),
        )
    }

    private data class Fixture(
        val driver: JdbcSqliteDriver,
        val database: BudgetCalendarDatabase,
        val transactionRepository: TransactionRepositoryImpl,
        val creditCardRepository: CreditCardRepositoryImpl,
        val notificationRepository: NotificationRepositoryImpl,
        val accountRepository: AccountRepositoryImpl,
        val createAccountUseCase: CreateAccountUseCase,
        val createTransactionUseCase: CreateTransactionUseCase,
        val updateTransactionStatusUseCase: UpdateTransactionStatusUseCase,
        val markOverdueTransactionsUseCase: MarkOverdueTransactionsUseCase,
        val generateNotificationsUseCase: GenerateNotificationsUseCase,
        val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    )
}
