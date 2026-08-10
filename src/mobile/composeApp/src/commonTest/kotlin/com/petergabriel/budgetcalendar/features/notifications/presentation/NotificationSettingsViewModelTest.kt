package com.petergabriel.budgetcalendar.features.notifications.presentation

import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.GetUserSettingsUseCase
import com.petergabriel.budgetcalendar.features.notifications.domain.usecase.UpdateUserSettingsUseCase
import com.petergabriel.budgetcalendar.features.notifications.testutil.FakeUserSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeUserSettingsRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeUserSettingsRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptySettings_loadsDefault15() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("15", viewModel.uiState.value.thresholdInput)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.loadError)
    }

    @Test
    fun loadFailure_keepsUnknownValueUnavailable_andRetryCanRecover() = runTest(dispatcher) {
        repository.failReads = true
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.thresholdInput)
        assertNotNull(viewModel.uiState.value.loadError)

        repository.failReads = false
        viewModel.retry()
        advanceUntilIdle()

        assertEquals("15", viewModel.uiState.value.thresholdInput)
        assertEquals(null, viewModel.uiState.value.loadError)
    }

    @Test
    fun edit_clearsValidationSaveErrorAndSuccess() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onThresholdInputChange("0")
        viewModel.save()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.validationError)

        viewModel.onThresholdInputChange("25")
        repository.failWrites = true
        viewModel.save()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.saveError)

        viewModel.onThresholdInputChange("30")
        assertEquals(null, viewModel.uiState.value.validationError)
        assertEquals(null, viewModel.uiState.value.saveError)
        assertFalse(viewModel.uiState.value.didSave)
    }

    @Test
    fun saveSuccess_setsIndicator_thenEditClearsIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onThresholdInputChange("42")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.didSave)
        assertEquals(1, repository.getWriteCount())

        viewModel.onThresholdInputChange("43")
        assertFalse(viewModel.uiState.value.didSave)
    }

    private fun viewModel(): NotificationSettingsViewModel {
        return NotificationSettingsViewModel(
            getUserSettingsUseCase = GetUserSettingsUseCase(repository),
            updateUserSettingsUseCase = UpdateUserSettingsUseCase(repository),
        )
    }
}
