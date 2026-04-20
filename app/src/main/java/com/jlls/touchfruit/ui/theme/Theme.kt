package com.jlls.touchfruit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ===============================
// TOUCHFRUIT THEME
// ===============================

private val TouchFruitColorScheme = lightColorScheme(
    primary = ActionBg,
    onPrimary = ActionFg,
    secondary = TextSecondary,
    onSecondary = BgSurface,
    tertiary = CardBlue,
    onTertiary = TextPrimary,
    background = BgCanvas,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgSearch,
    onSurfaceVariant = TextSecondary
)

@Composable
fun TouchFruitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TouchFruitColorScheme,
        typography = Typography,
        content = content
    )
}