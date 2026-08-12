package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BcDarkColors
import com.petergabriel.budgetcalendar.core.designsystem.theme.BcLightColors
import kotlin.test.Test
import kotlin.test.assertEquals

class BcButtonTest {

    @Test
    fun variants_preserveTransparentContainersAndDimSolidColors() {
        listOf(BcLightColors, BcDarkColors).forEach { colors ->
            ButtonVariant.values().forEach { variant ->
                val token = variantToken(
                    variant = variant,
                    colors = colors,
                    borderWidth = 1.dp,
                )
                val expectedContainer = when (variant) {
                    ButtonVariant.Primary,
                    ButtonVariant.PrimaryIconLeading,
                    -> colors.bgDark

                    ButtonVariant.Outline,
                    ButtonVariant.OutlineIconTrailing,
                    ButtonVariant.Ghost,
                    -> Color.Transparent

                    ButtonVariant.Destructive -> colors.colorError
                }
                val expectedContent = when (variant) {
                    ButtonVariant.Primary,
                    ButtonVariant.PrimaryIconLeading,
                    ButtonVariant.Destructive,
                    -> colors.textInverted

                    ButtonVariant.Outline,
                    ButtonVariant.OutlineIconTrailing,
                    ButtonVariant.Ghost,
                    -> colors.textPrimary
                }

                listOf(true, false).forEach { enabled ->
                    val alpha = if (enabled) 1f else 0.6f
                    assertEquals(
                        expected = if (expectedContainer == Color.Transparent) {
                            expectedContainer
                        } else {
                            expectedContainer.copy(alpha = alpha)
                        },
                        actual = resolveButtonContainerColor(token.containerColor, alpha),
                        message = "$variant container color for enabled=$enabled",
                    )
                    assertEquals(
                        expected = expectedContent.copy(alpha = alpha),
                        actual = token.contentColor.copy(alpha = alpha),
                        message = "$variant content color for enabled=$enabled",
                    )
                }
            }
        }
    }
}
