package com.petergabriel.budgetcalendar.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import myapplication.composeapp.generated.resources.Res
import myapplication.composeapp.generated.resources.inter_medium
import myapplication.composeapp.generated.resources.inter_regular
import myapplication.composeapp.generated.resources.inter_semibold
import myapplication.composeapp.generated.resources.outfit_black
import myapplication.composeapp.generated.resources.outfit_bold
import myapplication.composeapp.generated.resources.outfit_extrabold
import myapplication.composeapp.generated.resources.outfit_medium
import myapplication.composeapp.generated.resources.outfit_regular
import myapplication.composeapp.generated.resources.outfit_semibold
import org.jetbrains.compose.resources.Font

@Immutable
data class BcTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val cardTitle: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val calendarDay: TextStyle,
    val calendarAmount: TextStyle,
)

@Composable
internal fun rememberBcTypography(): BcTypography {
    val outfitFamily = FontFamily(
        Font(resource = Res.font.outfit_regular, weight = FontWeight.Normal),
        Font(resource = Res.font.outfit_medium, weight = FontWeight.Medium),
        Font(resource = Res.font.outfit_semibold, weight = FontWeight.SemiBold),
        Font(resource = Res.font.outfit_bold, weight = FontWeight.Bold),
        Font(resource = Res.font.outfit_extrabold, weight = FontWeight.ExtraBold),
        Font(resource = Res.font.outfit_black, weight = FontWeight.Black),
    )

    val interFamily = FontFamily(
        Font(resource = Res.font.inter_regular, weight = FontWeight.Normal),
        Font(resource = Res.font.inter_medium, weight = FontWeight.Medium),
        Font(resource = Res.font.inter_semibold, weight = FontWeight.SemiBold),
    )

    return remember(outfitFamily, interFamily) {
        buildBcTypography(outfitFamily = outfitFamily, interFamily = interFamily)
    }
}

private val FallbackBcTypography = buildBcTypography(
    outfitFamily = FontFamily.SansSerif,
    interFamily = FontFamily.SansSerif,
)

val LocalBcTypography = staticCompositionLocalOf { FallbackBcTypography }

private fun buildBcTypography(
    outfitFamily: FontFamily,
    interFamily: FontFamily,
): BcTypography {
    return BcTypography(
        displayLarge = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.Black,
            fontSize = 72.sp,
            lineHeight = 72.sp,
            letterSpacing = (-3).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.Black,
            fontSize = 56.sp,
            lineHeight = 56.sp,
            letterSpacing = (-2).sp,
        ),
        headline = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1).sp,
        ),
        title = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
        ),
        section = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.5).sp,
        ),
        cardTitle = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = interFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = interFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = interFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        caption = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.sp,
        ),
        calendarDay = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            lineHeight = 32.sp,
        ),
        calendarAmount = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        ),
    )
}
