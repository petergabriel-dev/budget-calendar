package com.petergabriel.budgetcalendar.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = BudgetCalendarDatabase.Schema,
            context = get<Context>(),
            name = "budget_calendar.db",
        )
    }
}
