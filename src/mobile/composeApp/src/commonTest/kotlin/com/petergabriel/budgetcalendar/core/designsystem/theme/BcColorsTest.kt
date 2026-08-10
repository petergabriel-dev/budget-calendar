package com.petergabriel.budgetcalendar.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BcColorsTest {

    @Test
    fun palettesExposeApprovedSemanticValues() {
        assertEquals(Color(0xFF000000), BcLightColors.bgDark)
        assertEquals(Color(0xFF000000), BcLightColors.focusRing)
        assertEquals(Color(0xFF92400E), BcLightColors.colorWarningText)
        assertEquals(Color(0xFF1D4ED8), BcLightColors.colorInfoText)

        assertEquals(Color(0xFF09090B), BcDarkColors.bgPrimary)
        assertEquals(Color(0xFFFAFAFA), BcDarkColors.bgDark)
        assertEquals(Color(0xFFFAFAFA), BcDarkColors.focusRing)
        assertEquals(Color(0xFFFDE68A), BcDarkColors.colorWarningText)
        assertEquals(Color(0xFFBFDBFE), BcDarkColors.colorInfoText)
    }

    @Test
    fun semanticTextPairsMeetWcagAaContrast() {
        listOf(BcLightColors, BcDarkColors).forEach { colors ->
            assertAa(colors.textPrimary, colors.bgPrimary)
            assertAa(colors.textInverted, colors.bgDark)
            assertAa(colors.colorSuccessText, colors.colorSuccessBg)
            assertAa(colors.colorErrorText, colors.colorErrorBg)
            assertAa(colors.colorWarningText, colors.colorWarningBg)
            assertAa(colors.colorInfoText, colors.colorInfoBg)
        }
    }

    private fun assertAa(foreground: Color, background: Color) {
        assertTrue(
            actual = contrastRatio(foreground, background) >= 4.5,
            message = "Contrast ${contrastRatio(foreground, background)} is below WCAG AA",
        )
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (minOf(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        return (0.2126 * linearize(color.red)) +
            (0.7152 * linearize(color.green)) +
            (0.0722 * linearize(color.blue))
    }
}
