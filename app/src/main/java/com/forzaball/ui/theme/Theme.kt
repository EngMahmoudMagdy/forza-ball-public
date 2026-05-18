package com.forzaball.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ForzaBallPrimary,
    onPrimary = Color(0xFFFFFFFF),
    secondary = ForzaBallSurfaceContainerHighest,
    tertiary = ForzaBallError,
    onTertiary = Color(0xFFFFFFFF),
    background = ForzaBallBackgroundDark,
    onBackground = ForzaBallOnSurface,
    surface = ForzaBallBackgroundDark,
    onSurface = ForzaBallOnSurface,
    onSurfaceVariant = ForzaBallOnSurfaceVariant,
    surfaceVariant = ForzaBallSurfaceContainerLow,
    outline = ForzaBallOutline,
    error = ForzaBallError,
    errorContainer = ForzaBallErrorContainer,
    primaryContainer = ForzaBallPrimaryContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = ForzaBallPrimary,
    onPrimary = Color(0xFFFFFFFF),
    secondary = PurpleGrey40,
    tertiary = ForzaBallError,
    background = ForzaBallBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = ForzaBallBackgroundLight,
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8),
    error = ForzaBallError,
)

@Composable
fun ForzaBallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}