package com.petergabriel.budgetcalendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    CALENDAR("Calendar", Icons.Outlined.DateRange),
    ACCOUNTS("Accounts", Icons.Outlined.CreditCard),
    ME("Me", Icons.Outlined.Person),
}

data class AppNavigationState(
    val selectedTab: AppTab = AppTab.HOME,
    val pendingCalendarDate: String? = null,
)

fun AppNavigationState.openCalendarOn(date: LocalDate): AppNavigationState {
    return copy(
        selectedTab = AppTab.CALENDAR,
        pendingCalendarDate = date.toString(),
    )
}

fun AppNavigationState.consumePendingCalendarDate(): AppNavigationState {
    return copy(pendingCalendarDate = null)
}
