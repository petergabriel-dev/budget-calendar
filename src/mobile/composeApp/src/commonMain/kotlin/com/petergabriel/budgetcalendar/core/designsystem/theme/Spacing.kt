package com.petergabriel.budgetcalendar.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class BcSpacing(
    val spacing1: Dp,
    val spacing2: Dp,
    val spacing3: Dp,
    val spacing4: Dp,
    val spacing5: Dp,
    val spacing6: Dp,
    val spacing8: Dp,
    val spacing10: Dp,
    val spacing12: Dp,
)

internal val BcDefaultSpacing = BcSpacing(
    spacing1 = 4.dp,
    spacing2 = 8.dp,
    spacing3 = 12.dp,
    spacing4 = 16.dp,
    spacing5 = 20.dp,
    spacing6 = 24.dp,
    spacing8 = 32.dp,
    spacing10 = 40.dp,
    spacing12 = 48.dp,
)

val LocalBcSpacing = staticCompositionLocalOf { BcDefaultSpacing }
