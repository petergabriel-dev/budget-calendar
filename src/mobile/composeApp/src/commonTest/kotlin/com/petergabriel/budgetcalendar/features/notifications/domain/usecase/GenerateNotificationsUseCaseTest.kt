package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.domain.model.NotificationType
import com.petergabriel.budgetcalendar.features.notifications.testutil.FakeNotificationRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateNotificationsUseCaseTest {
    private lateinit var notificationRepository: FakeNotificationRepository
    private lateinit var useCase: GenerateNotificationsUseCase

    @BeforeTest
    fun setUp() {
        notificationRepository = FakeNotificationRepository()
        useCase = GenerateNotificationsUseCase(notificationRepository)
    }

    @Test
    fun overdue_firstCallInserts_secondCallUpdates_noDuplicate_andDismissedCanReinsert() = runBlocking {
        val overdues = listOf(sampleOverdueTransaction(1L), sampleOverdueTransaction(2L))

        useCase(
            overdueTransactions = overdues,
            daysRemaining = 6,
            projectedRollover = 0L,
            currentSts = 0L,
            poolBalance = 0L,
            thresholdPercent = 15,
            nowMillis = 1_000L,
        )

        val first = notificationRepository.allNotifications()
        assertEquals(1, first.count { it.type == NotificationType.OVERDUE_TRANSACTIONS })
        assertEquals(2, first.first { it.type == NotificationType.OVERDUE_TRANSACTIONS }.overdueCount)

        useCase(
            overdueTransactions = overdues,
            daysRemaining = 6,
            projectedRollover = 0L,
            currentSts = 0L,
            poolBalance = 0L,
            thresholdPercent = 15,
            nowMillis = 2_000L,
        )

        val second = notificationRepository.allNotifications()
        assertEquals(1, second.count { it.type == NotificationType.OVERDUE_TRANSACTIONS })
        assertEquals(2_000L, second.first { it.type == NotificationType.OVERDUE_TRANSACTIONS }.updatedAt)

        val activeOverdue = second.first { it.type == NotificationType.OVERDUE_TRANSACTIONS && !it.isDismissed }
        notificationRepository.dismiss(activeOverdue.id)

        useCase(
            overdueTransactions = overdues,
            daysRemaining = 6,
            projectedRollover = 0L,
            currentSts = 0L,
            poolBalance = 0L,
            thresholdPercent = 15,
            nowMillis = 3_000L,
        )

        val third = notificationRepository.allNotifications()
        assertEquals(2, third.count { it.type == NotificationType.OVERDUE_TRANSACTIONS })
        assertEquals(1, third.count { it.type == NotificationType.OVERDUE_TRANSACTIONS && !it.isDismissed })
    }

    @Test
    fun rollover_generatesAt7_5_3_skips6_dedupesWithinMonth_andRegeneratesNextMonth() = runBlocking {
        val januaryNow = millisFor(2026, 1, 24)
        val februaryNow = millisFor(2026, 2, 22)

        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 6,
            projectedRollover = 50_00L,
            currentSts = 100L,
            poolBalance = 1_000L,
            thresholdPercent = 15,
            nowMillis = januaryNow,
        )
        assertTrue(notificationRepository.allNotifications().none { it.type.name.startsWith("ROLLOVER") })

        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 7,
            projectedRollover = 80_00L,
            currentSts = 100L,
            poolBalance = 1_000L,
            thresholdPercent = 15,
            nowMillis = januaryNow,
        )
        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 5,
            projectedRollover = 60_00L,
            currentSts = 100L,
            poolBalance = 1_000L,
            thresholdPercent = 15,
            nowMillis = januaryNow,
        )
        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 3,
            projectedRollover = 40_00L,
            currentSts = 100L,
            poolBalance = 1_000L,
            thresholdPercent = 15,
            nowMillis = januaryNow,
        )

        val afterFirstMonth = notificationRepository.allNotifications()
        assertEquals(1, afterFirstMonth.count { it.type == NotificationType.ROLLOVER_REMINDER_7 })
        assertEquals(1, afterFirstMonth.count { it.type == NotificationType.ROLLOVER_REMINDER_5 })
        assertEquals(1, afterFirstMonth.count { it.type == NotificationType.ROLLOVER_REMINDER_3 })

        val januaryR7 = afterFirstMonth.first { it.type == NotificationType.ROLLOVER_REMINDER_7 }
        notificationRepository.dismiss(januaryR7.id)

        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 7,
            projectedRollover = 80_00L,
            currentSts = 100L,
            poolBalance = 1_000L,
            thresholdPercent = 15,
            nowMillis = januaryNow + 1L,
        )
        assertEquals(1, notificationRepository.allNotifications().count { it.type == NotificationType.ROLLOVER_REMINDER_7 })

        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 7,
            projectedRollover = 70_00L,
            currentSts = 100L,
            poolBalance = 1_000L,
            thresholdPercent = 15,
            nowMillis = februaryNow,
        )

        assertEquals(2, notificationRepository.allNotifications().count { it.type == NotificationType.ROLLOVER_REMINDER_7 })
    }

    @Test
    fun stsLow_andStsNegative_behaveWithThresholdRules_andCoexist() = runBlocking {
        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 6,
            projectedRollover = 0L,
            currentSts = 15L,
            poolBalance = 100L,
            thresholdPercent = 15,
            nowMillis = 1_000L,
        )
        assertTrue(notificationRepository.allNotifications().none { it.type == NotificationType.STS_LOW })

        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 6,
            projectedRollover = 0L,
            currentSts = 0L,
            poolBalance = 0L,
            thresholdPercent = 15,
            nowMillis = 2_000L,
        )
        assertTrue(notificationRepository.allNotifications().none { it.type == NotificationType.STS_LOW })

        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 6,
            projectedRollover = 0L,
            currentSts = 14L,
            poolBalance = 100L,
            thresholdPercent = 15,
            nowMillis = 3_000L,
        )
        assertEquals(1, notificationRepository.allNotifications().count { it.type == NotificationType.STS_LOW })

        useCase(
            overdueTransactions = emptyList(),
            daysRemaining = 6,
            projectedRollover = 0L,
            currentSts = -10L,
            poolBalance = 100L,
            thresholdPercent = 15,
            nowMillis = 4_000L,
        )

        val all = notificationRepository.allNotifications()
        assertEquals(1, all.count { it.type == NotificationType.STS_LOW && !it.isDismissed })
        assertEquals(1, all.count { it.type == NotificationType.STS_NEGATIVE && !it.isDismissed })
    }

    @Test
    fun idempotent_sameInputsTwice_producesSameState() = runBlocking {
        val overdues = listOf(sampleOverdueTransaction(1L))
        val nowMillis = millisFor(2026, 3, 25)

        useCase(
            overdueTransactions = overdues,
            daysRemaining = 7,
            projectedRollover = 123_45L,
            currentSts = -50L,
            poolBalance = 500L,
            thresholdPercent = 15,
            nowMillis = nowMillis,
        )
        val firstState = notificationRepository.allNotifications()

        useCase(
            overdueTransactions = overdues,
            daysRemaining = 7,
            projectedRollover = 123_45L,
            currentSts = -50L,
            poolBalance = 500L,
            thresholdPercent = 15,
            nowMillis = nowMillis,
        )
        val secondState = notificationRepository.allNotifications()

        assertEquals(firstState, secondState)
    }

    private fun sampleOverdueTransaction(id: Long): Transaction {
        return Transaction(
            id = id,
            accountId = 1L,
            destinationAccountId = null,
            amount = 1_000L,
            signedAmount = -1_000L,
            date = millisFor(2026, 1, 1),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.OVERDUE,
            description = "Overdue #$id",
            category = "Bills",
            linkedTransactionId = null,
            isSandbox = false,
            createdAt = millisFor(2026, 1, 1),
            updatedAt = millisFor(2026, 1, 1),
        )
    }

    private fun millisFor(year: Int, month: Int, day: Int): Long {
        return LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    }
}
