package com.petergabriel.budgetcalendar.features.sandbox.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SandboxScreen(
    modifier: Modifier = Modifier,
    viewModel: SandboxViewModel = koinViewModel(),
) {
    DisposableEffect(Unit) {
        viewModel.setSandboxMode(true)
        onDispose { viewModel.setSandboxMode(false) }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Sandbox mode is available from the Home tab.")
    }
}
