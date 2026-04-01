package com.petergabriel.budgetcalendar.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class BcShadows(
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
)

internal val BcDefaultShadows = BcShadows(
    sm = 1.dp,
    md = 4.dp,
    lg = 10.dp,
    xl = 20.dp,
)

val LocalBcShadows = staticCompositionLocalOf { BcDefaultShadows }
