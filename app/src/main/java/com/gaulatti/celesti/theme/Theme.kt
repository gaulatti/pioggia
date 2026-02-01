package com.gaulatti.celesti.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CelestiTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isInDarkTheme) {
        darkColorScheme(
            primary = DarkModeAccentBlue,
            secondary = DarkModeDesert,
            tertiary = DarkModeSunset,
            background = DarkLightSand,
            surface = DarkModeSand,
            onPrimary = DarkModeTextPrimary,
            onSecondary = DarkModeTextPrimary,
            onBackground = DarkModeTextPrimary,
            onSurface = DarkModeTextPrimary
        )
    } else {
        lightColorScheme(
            primary = Sea,
            secondary = Desert,
            tertiary = Sunset,
            background = LightSand,
            surface = Sand,
            onPrimary = Color.White,
            onSecondary = TextPrimary,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}