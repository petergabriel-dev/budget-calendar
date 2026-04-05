package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarDayCellTest {

    private val selectedBackground = Color(0xFF101010)
    private val successTint = Color(0x3322C55E)
    private val errorTint = Color(0x33EF4444)
    private val todayBorder = Color(0xFF000000)
    private val transparent = Color.Transparent

    @Test
    fun resolveColors_nonTodayPositiveNet_usesSuccessTintBackgroundAndTransparentBorder() {
        val background = resolveCalendarDayCellBackground(
            isSelected = false,
            isToday = false,
            netAmount = 500L,
            selectedBackground = selectedBackground,
            successTint = successTint,
            errorTint = errorTint,
            baseBackground = transparent,
        )
        val border = resolveCalendarDayCellBorderColor(
            isToday = false,
            todayBorder = todayBorder,
        )

        assertEquals(successTint, background)
        assertEquals(transparent, border)
    }

    @Test
    fun resolveColors_nonTodayNegativeNet_usesErrorTintBackgroundAndTransparentBorder() {
        val background = resolveCalendarDayCellBackground(
            isSelected = false,
            isToday = false,
            netAmount = -300L,
            selectedBackground = selectedBackground,
            successTint = successTint,
            errorTint = errorTint,
            baseBackground = transparent,
        )
        val border = resolveCalendarDayCellBorderColor(
            isToday = false,
            todayBorder = todayBorder,
        )

        assertEquals(errorTint, background)
        assertEquals(transparent, border)
    }

    @Test
    fun resolveColors_selectedNonTodayWithNonZeroNet_selectedBackgroundWins() {
        val background = resolveCalendarDayCellBackground(
            isSelected = true,
            isToday = false,
            netAmount = 900L,
            selectedBackground = selectedBackground,
            successTint = successTint,
            errorTint = errorTint,
            baseBackground = transparent,
        )
        val border = resolveCalendarDayCellBorderColor(
            isToday = false,
            todayBorder = todayBorder,
        )

        assertEquals(selectedBackground, background)
        assertEquals(transparent, border)
    }
}
