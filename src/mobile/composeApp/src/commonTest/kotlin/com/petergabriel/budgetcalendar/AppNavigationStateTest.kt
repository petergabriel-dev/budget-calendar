package com.petergabriel.budgetcalendar

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppNavigationStateTest {

    @Test
    fun neighborUsesTabOrderWithoutWrapping() {
        assertEquals(AppTab.CALENDAR, AppTab.HOME.neighbor(1))
        assertEquals(AppTab.ME, AppTab.ME.neighbor(1))
        assertEquals(AppTab.HOME, AppTab.HOME.neighbor(-1))
        assertEquals(AppTab.CALENDAR, AppTab.ACCOUNTS.neighbor(-1))
    }

    @Test
    fun openCalendarOnPreservesExactDateThenConsumeClearsIt() {
        val selectedDate = LocalDate(2026, 8, 18)
        val handedOff = AppNavigationState().openCalendarOn(selectedDate)

        assertEquals(AppTab.CALENDAR, handedOff.selectedTab)
        assertEquals("2026-08-18", handedOff.pendingCalendarDate)
        assertNull(handedOff.consumePendingCalendarDate().pendingCalendarDate)
    }
}
