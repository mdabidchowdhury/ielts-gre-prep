package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SophisticatedDarkPrimary,
    secondary = SophisticatedDarkTextSecondary,
    tertiary = SophisticatedDarkPrimary,
    background = SophisticatedDarkBg,
    surface = SophisticatedDarkCard,
    onBackground = SophisticatedDarkText,
    onSurface = SophisticatedDarkText,
    outlineVariant = SophisticatedDarkOutline,
    primaryContainer = SophisticatedDarkPrimaryContainer,
    onPrimaryContainer = SophisticatedDarkPrimary,
    error = MistakeText,
    errorContainer = MistakeBg,
    onErrorContainer = MistakeText
)

private val LightColorScheme = lightColorScheme(
    primary = SlateBluePrimaryLight,
    secondary = SlateBlueSecondaryLight,
    tertiary = SlateBlueTertiaryLight,
    background = LightCanvas,
    surface = LightCardSurface,
    onBackground = LightText,
    onSurface = LightText,
    primaryContainer = SlateBluePrimaryLight.copy(alpha = 0.1f),
    onPrimaryContainer = SlateBluePrimaryLight,
    error = androidx.compose.ui.graphics.Color(0xFFD32F2F),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFEBEE),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFC62828)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // disabled to lock premium academic theme
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
