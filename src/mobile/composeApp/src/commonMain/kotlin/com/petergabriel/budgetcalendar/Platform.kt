package com.petergabriel.budgetcalendar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform