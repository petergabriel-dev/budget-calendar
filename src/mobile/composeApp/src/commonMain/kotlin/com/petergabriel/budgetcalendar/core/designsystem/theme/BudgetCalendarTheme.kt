package com.petergabriel.budgetcalendar.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BudgetCalendarTheme(
    content: @Composable () -> Unit,
) {
    val colors = remember { BcLightColors }
    val typography = rememberBcTypography()
    val spacing = remember { BcDefaultSpacing }
    val radius = remember { BcDefaultRadius }
    val shadows = remember { BcDefaultShadows }

    CompositionLocalProvider(
        LocalBcColors provides colors,
        LocalBcTypography provides typography,
        LocalBcSpacing provides spacing,
        LocalBcRadius provides radius,
        LocalBcShadows provides shadows,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColors(),
            typography = typography.toMaterialTypography(),
            content = content,
        )
    }
}

private fun BcColors.toMaterialColors(): ColorScheme = lightColorScheme(
    primary = bgDark,
    onPrimary = textInverted,
    primaryContainer = bgSurface,
    onPrimaryContainer = textPrimary,
    inversePrimary = textInverted,
    secondary = bgMuted,
    onSecondary = textInverted,
    secondaryContainer = colorSuccessBg,
    onSecondaryContainer = colorSuccess,
    tertiary = colorInfo,
    onTertiary = textInverted,
    tertiaryContainer = colorInfoBg,
    onTertiaryContainer = colorInfo,
    background = bgPrimary,
    onBackground = textPrimary,
    surface = bgPrimary,
    onSurface = textPrimary,
    surfaceVariant = bgSurface,
    onSurfaceVariant = textSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = bgDark,
    inverseOnSurface = textInverted,
    error = colorError,
    onError = textInverted,
    errorContainer = colorErrorBg,
    onErrorContainer = colorError,
    outline = borderStrong,
    outlineVariant = borderSubtle,
    scrim = Color.Black.copy(alpha = 0.45f),
)

private fun BcTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = title,
    headlineLarge = headline,
    headlineMedium = title,
    headlineSmall = section,
    titleLarge = section,
    titleMedium = cardTitle,
    titleSmall = bodyLarge.copy(fontFamily = bodyLarge.fontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = bodyMedium.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = caption,
    labelSmall = bodySmall,
)

object BudgetCalendarTheme {
    val colors: BcColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBcColors.current

    val typography: BcTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalBcTypography.current

    val spacing: BcSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalBcSpacing.current

    val radius: BcRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalBcRadius.current

    val shadows: BcShadows
        @Composable
        @ReadOnlyComposable
        get() = LocalBcShadows.current
}
