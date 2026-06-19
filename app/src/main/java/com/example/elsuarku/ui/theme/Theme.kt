package com.example.elsuarku.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// ElSuarKu Color Schemes — Deep Blue + Emerald Green + Gold
// ============================================================

// Defined before color schemes (used by them)
private val Color_Holo_Red_Light = androidx.compose.ui.graphics.Color(0xFFFFCDD2)
private val Color_Holo_Red_Dark = androidx.compose.ui.graphics.Color(0xFFB71C1C)

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = OnDeepBlue,
    primaryContainer = DeepBlueSurface,
    onPrimaryContainer = DeepBlueDark,

    secondary = EmeraldGreen,
    onSecondary = OnEmeraldGreen,
    secondaryContainer = EmeraldGreenSurface,
    onSecondaryContainer = EmeraldGreenDark,

    tertiary = Gold,
    onTertiary = OnGold,
    tertiaryContainer = GoldSurface,
    onTertiaryContainer = GoldDark,

    background = SoftWhite,
    onBackground = OnSurface,
    surface = SurfaceWhite,
    onSurface = OnSurface,
    surfaceVariant = DeepBlueSurface,
    onSurfaceVariant = DeepBlueLight,

    error = StatusError,
    onError = OnDeepBlue,
    errorContainer = Color_Holo_Red_Light,
    onErrorContainer = StatusError,

    outline = DeepBlueLight.copy(alpha = 0.3f),
    outlineVariant = DeepBlueLight.copy(alpha = 0.12f),

    inverseSurface = DeepBlueDark,
    inverseOnSurface = OnDeepBlue,
    inversePrimary = DeepBlueLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = DeepBlueLight,
    onPrimary = DeepBlueDark,
    primaryContainer = DeepBlue,
    onPrimaryContainer = DeepBlueSurface,

    secondary = EmeraldGreenLight,
    onSecondary = EmeraldGreenDark,
    secondaryContainer = EmeraldGreen,
    onSecondaryContainer = EmeraldGreenSurface,

    tertiary = GoldLight,
    onTertiary = GoldDark,
    tertiaryContainer = Gold,
    onTertiaryContainer = OnGold,

    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDarkElevated,
    onSurface = OnSurfaceDark,
    surfaceVariant = DeepBlueDark,
    onSurfaceVariant = DeepBlueLight,

    error = StatusError,
    onError = OnDeepBlue,
    errorContainer = Color_Holo_Red_Dark,
    onErrorContainer = StatusError,

    outline = DeepBlueLight.copy(alpha = 0.4f),
    outlineVariant = DeepBlueLight.copy(alpha = 0.15f),

    inverseSurface = SoftWhite,
    inverseOnSurface = OnSurface,
    inversePrimary = DeepBlue,
)

// ============================================================
// ElSuarKu Theme Composable
// ============================================================

@Composable
fun ElSuarKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled — we use our brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Dynamic color available but we prefer brand colors
            // Keeping this path for potential future use
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Set status bar and navigation bar colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepBlueDark.toArgb()
            window.navigationBarColor = DeepBlueDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
