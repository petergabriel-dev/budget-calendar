package com.petergabriel.budgetcalendar.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BcColors(
    val bgDark: Color,
    val bgMuted: Color,
    val bgPrimary: Color,
    val bgSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInverted: Color,
    val textDisabled: Color,
    val borderStrong: Color,
    val borderSubtle: Color,
    val colorSuccess: Color,
    val colorSuccessBg: Color,
    val colorError: Color,
    val colorErrorBg: Color,
    val colorWarning: Color,
    val colorWarningBg: Color,
    val colorInfo: Color,
    val colorInfoBg: Color,
)

internal val BcLightColors = BcColors(
    bgDark = Color(0xFF000000),
    bgMuted = Color(0xFF27272A),
    bgPrimary = Color(0xFFFFFFFF),
    bgSurface = Color(0xFFF4F4F5),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF71717A),
    textTertiary = Color(0xFFA1A1AA),
    textInverted = Color(0xFFFFFFFF),
    textDisabled = Color(0xFFD4D4D8),
    borderStrong = Color(0xFFE4E4E7),
    borderSubtle = Color(0xFFF4F4F5),
    colorSuccess = Color(0xFF22C55E),
    colorSuccessBg = Color(0xFFDCFCE7),
    colorError = Color(0xFFEF4444),
    colorErrorBg = Color(0xFFFEE2E2),
    colorWarning = Color(0xFFF59E0B),
    colorWarningBg = Color(0xFFFEF3C7),
    colorInfo = Color(0xFF3B82F6),
    colorInfoBg = Color(0xFFDBEAFE),
)

val LocalBcColors = staticCompositionLocalOf { BcLightColors }
