package com.petergabriel.budgetcalendar

import androidx.compose.ui.window.ComposeUIViewController
import com.petergabriel.budgetcalendar.di.initKoin
import org.koin.mp.KoinPlatform

fun MainViewController() = ComposeUIViewController {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoin()
    }
    App()
}
