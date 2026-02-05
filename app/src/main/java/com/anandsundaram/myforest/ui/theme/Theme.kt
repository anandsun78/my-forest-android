package com.anandsundaram.myforest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Moss,
    secondary = Clay,
    tertiary = Sand,
    background = Night,
    surface = Pine,
    surfaceVariant = Color(0xFF1A201C),
    onPrimary = Mist,
    onSecondary = Night,
    onTertiary = Night,
    onBackground = Mist,
    onSurface = Mist
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    secondary = Moss,
    tertiary = Clay,
    background = Mist,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Sand,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Ink,
    onTertiary = Ink,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun MyForestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
