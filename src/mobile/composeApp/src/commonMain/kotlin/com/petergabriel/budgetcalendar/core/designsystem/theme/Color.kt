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
    val focusRing: Color,
    val borderStrong: Color,
    val borderSubtle: Color,
    val colorSuccess: Color,
    val colorSuccessText: Color,
    val colorSuccessBg: Color,
    val colorError: Color,
    val colorErrorText: Color,
    val colorErrorBg: Color,
    val colorWarning: Color,
    val colorWarningText: Color,
    val colorWarningBg: Color,
    val colorInfo: Color,
    val colorInfoText: Color,
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
    focusRing = Color(0xFF000000),
    borderStrong = Color(0xFFE4E4E7),
    borderSubtle = Color(0xFFF4F4F5),
    colorSuccess = Color(0xFF22C55E),
    colorSuccessText = Color(0xFF2E7D32),
    colorSuccessBg = Color(0xFFDCFCE7),
    colorError = Color(0xFFEF4444),
    colorErrorText = Color(0xFFC62828),
    colorErrorBg = Color(0xFFFEE2E2),
    colorWarning = Color(0xFFF59E0B),
    colorWarningText = Color(0xFF92400E),
    colorWarningBg = Color(0xFFFEF3C7),
    colorInfo = Color(0xFF3B82F6),
    colorInfoText = Color(0xFF1D4ED8),
    colorInfoBg = Color(0xFFDBEAFE),
)

internal val BcDarkColors = BcColors(
    bgDark = Color(0xFFFAFAFA),
    bgMuted = Color(0xFF27272A),
    bgPrimary = Color(0xFF09090B),
    bgSurface = Color(0xFF18181B),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF71717A),
    textInverted = Color(0xFF09090B),
    textDisabled = Color(0xFF52525B),
    focusRing = Color(0xFFFAFAFA),
    borderStrong = Color(0xFF3F3F46),
    borderSubtle = Color(0xFF27272A),
    colorSuccess = Color(0xFF4ADE80),
    colorSuccessText = Color(0xFF86EFAC),
    colorSuccessBg = Color(0xFF14532D),
    colorError = Color(0xFFF87171),
    colorErrorText = Color(0xFFFCA5A5),
    colorErrorBg = Color(0xFF7F1D1D),
    colorWarning = Color(0xFFFBBF24),
    colorWarningText = Color(0xFFFDE68A),
    colorWarningBg = Color(0xFF78350F),
    colorInfo = Color(0xFF60A5FA),
    colorInfoText = Color(0xFFBFDBFE),
    colorInfoBg = Color(0xFF1E3A8A),
)

val LocalBcColors = staticCompositionLocalOf { BcLightColors }
