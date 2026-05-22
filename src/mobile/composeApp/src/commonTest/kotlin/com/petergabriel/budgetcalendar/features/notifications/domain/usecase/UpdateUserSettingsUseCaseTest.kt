package com.petergabriel.budgetcalendar.features.notifications.domain.usecase

import com.petergabriel.budgetcalendar.features.notifications.testutil.FakeUserSettingsRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateUserSettingsUseCaseTest {
    private lateinit var userSettingsRepository: FakeUserSettingsRepository
    private lateinit var useCase: UpdateUserSettingsUseCase

    @BeforeTest
    fun setUp() {
        userSettingsRepository = FakeUserSettingsRepository()
        useCase = UpdateUserSettingsUseCase(userSettingsRepository)
    }

    @Test
    fun thresholdBelowRange_returnsError_andDoesNotWrite() = runBlocking {
        val result = useCase(0)

        assertTrue(result.isFailure)
        assertEquals(0, userSettingsRepository.getWriteCount())
    }

    @Test
    fun thresholdAboveRange_returnsError_andDoesNotWrite() = runBlocking {
        val result = useCase(101)

        assertTrue(result.isFailure)
        assertEquals(0, userSettingsRepository.getWriteCount())
    }

    @Test
    fun thresholdAtLowerBoundary_succeeds() = runBlocking {
        val result = useCase(1)

        assertTrue(result.isSuccess)
        assertEquals(1, userSettingsRepository.getWriteCount())
    }

    @Test
    fun thresholdAtUpperBoundary_succeeds() = runBlocking {
        val result = useCase(100)

        assertTrue(result.isSuccess)
        assertEquals(1, userSettingsRepository.getWriteCount())
    }

    @Test
    fun thresholdDefaultValue_succeeds() = runBlocking {
        val result = useCase(15)

        assertTrue(result.isSuccess)
        assertEquals(1, userSettingsRepository.getWriteCount())
    }
}
