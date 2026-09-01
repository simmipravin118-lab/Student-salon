package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SalonGoldPrimary,
    onPrimary = TextDarkPrimary,
    primaryContainer = SalonDarkSubCardSurface,
    onPrimaryContainer = SalonGoldLight,
    secondary = StatusRejoinedCyan,
    onSecondary = TextDarkPrimary,
    tertiary = StatusServingGreen,
    onTertiary = TextDarkPrimary,
    background = SalonDarkNavyBackground,
    onBackground = TextWhitePrimary,
    surface = SalonDarkNavySurface,
    onSurface = TextWhitePrimary,
    surfaceVariant = SalonDarkCardSurface,
    onSurfaceVariant = TextGraySecondary,
    outline = SalonDarkCardBorder,
    outlineVariant = SalonDarkSubCardBorder,
    error = StatusCancelledRed,
    onError = TextDarkPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = SalonGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    tertiary = StatusServingGreen,
    onTertiary = Color.White,
    background = SalonLightBackground,
    onBackground = TextDarkPrimary,
    surface = SalonLightSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextDarkSecondary,
    outline = SalonLightCardBorder,
    error = StatusCancelledRed,
    onError = Color.White
)

@Composable
fun StudentSalonTheme(
    darkTheme: Boolean = true, // Default to sleek barbershop dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
