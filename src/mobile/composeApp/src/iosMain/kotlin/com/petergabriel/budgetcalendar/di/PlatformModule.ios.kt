package com.petergabriel.budgetcalendar.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            name = "budget_calendar.db",
        )
    }
}
