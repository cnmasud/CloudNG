package com.cloudng.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = CloudTeal80,
    secondary        = CloudTealGrey80,
    tertiary         = CloudBlue80,
    background       = CloudSurface,
    surface          = CloudSurface,
    surfaceVariant   = CloudSurfaceVar,
    onBackground     = CloudOnSurface,
    onSurface        = CloudOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary          = CloudTeal40,
    secondary        = CloudTealGrey40,
    tertiary         = CloudBlue40
)

@Composable
fun CloudNGTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CloudNGTypography,
        content = content
    )
}
