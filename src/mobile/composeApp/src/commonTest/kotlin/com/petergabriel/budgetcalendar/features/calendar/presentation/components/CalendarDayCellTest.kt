package com.petergabriel.budgetcalendar.features.calendar.presentation.components

import androidx.compose.ui.graphics.Color
import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarDay
import com.petergabriel.budgetcalendar.features.calendar.domain.model.DaySummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarDayCellTest {

    private val successTint = Color(0x3322C55E)
    private val errorTint = Color(0x33EF4444)
    private val todayBorder = Color(0xFF000000)
    private val transparent = Color.Transparent

    @Test
    fun contentDescription_includesDateSummaryAndStatuses() {
        val description = calendarDayContentDescription(
            CalendarDay(
                date = LocalDate(2026, 8, 18),
                isCurrentMonth = true,
                isToday = true,
                dailySummary = DaySummary(
                    totalIncome = 10_000L,
                    totalExpenses = 2_000L,
                    netAmount = 8_000L,
                    transactionCount = 2,
                    hasPending = true,
                    hasOverdue = true,
                    hasConfirmed = true,
                ),
            ),
        )

        kotlin.test.assertTrue(description.contains("Tuesday, August 18"))
        kotlin.test.assertTrue(description.contains("today"))
        kotlin.test.assertFalse(description.contains("selected"))
        kotlin.test.assertTrue(description.contains("2 transactions"))
        kotlin.test.assertTrue(description.contains("pending"))
        kotlin.test.assertTrue(description.contains("overdue"))
    }

    @Test
    fun resolveColors_nonTodayPositiveNet_usesSuccessTintBackgroundAndTransparentBorder() {
        val background = resolveCalendarDayCellBackground(
            isToday = false,
            netAmount = 500L,
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
            isToday = false,
            netAmount = -300L,
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

}
