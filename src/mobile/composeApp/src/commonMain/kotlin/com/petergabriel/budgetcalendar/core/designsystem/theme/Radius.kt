package com.petergabriel.budgetcalendar.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class BcRadius(
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val full: Dp,
)

internal val BcDefaultRadius = BcRadius(
    sm = 4.dp,
    md = 8.dp,
    lg = 12.dp,
    xl = 16.dp,
    xxl = 20.dp,
    full = 9_999.dp,
)

val LocalBcRadius = staticCompositionLocalOf { BcDefaultRadius }
