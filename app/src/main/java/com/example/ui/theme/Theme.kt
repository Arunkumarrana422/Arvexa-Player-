package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.domain.model.AppThemeColor
import com.example.domain.model.ThemePreference

fun createDarkColorScheme(palette: AppThemeColor): ColorScheme = darkColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.secondary,
    onPrimaryContainer = Color.White,
    secondary = palette.accent,
    onSecondary = NovaDarkBackground,
    secondaryContainer = NovaDarkCard,
    onSecondaryContainer = palette.accent,
    tertiary = palette.accent,
    onTertiary = if (palette.accent.luminance() > 0.5f) Color.Black else Color.White,
    background = NovaDarkBackground,
    onBackground = NovaTextPrimary,
    surface = NovaDarkSurface,
    onSurface = NovaTextPrimary,
    surfaceVariant = NovaDarkCard,
    onSurfaceVariant = NovaTextSecondary,
    error = NovaError,
    onError = Color.White
)

fun createLightColorScheme(palette: AppThemeColor): ColorScheme = lightColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.accent.copy(alpha = 0.2f),
    onPrimaryContainer = palette.primary,
    secondary = palette.secondary,
    onSecondary = Color.White,
    secondaryContainer = palette.accent.copy(alpha = 0.15f),
    onSecondaryContainer = palette.secondary,
    tertiary = palette.accent,
    onTertiary = if (palette.accent.luminance() > 0.5f) Color.Black else Color.White,
    background = NovaLightBackground,
    onBackground = NovaLightTextPrimary,
    surface = NovaLightSurface,
    onSurface = NovaLightTextPrimary,
    surfaceVariant = NovaLightCard,
    onSurfaceVariant = NovaLightTextSecondary,
    error = NovaError,
    onError = Color.White
)

val DarkColorScheme get() = createDarkColorScheme(ThemeColorManager.currentThemeColor)
val LightColorScheme get() = createLightColorScheme(ThemeColorManager.currentThemeColor)

@Composable
fun ArvexaPlayerTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    appThemeColor: AppThemeColor = AppThemeColor.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NovaPlayerTheme(
        themePreference = themePreference,
        appThemeColor = appThemeColor,
        dynamicColor = dynamicColor,
        content = content
    )
}

@Composable
fun NovaPlayerTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    appThemeColor: AppThemeColor = AppThemeColor.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ThemeColorManager.setAppThemeColor(appThemeColor)

    val darkTheme = isSystemInDarkTheme()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(appThemeColor)
        else -> createLightColorScheme(appThemeColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
