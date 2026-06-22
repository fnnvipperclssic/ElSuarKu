package com.example.elsuarku.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// ═══════════════════════════════════════════
// Light Color Scheme
// ═══════════════════════════════════════════
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
    errorContainer = StatusErrorSurface,
    onErrorContainer = StatusError,

    outline = DeepBlueLight.copy(alpha = 0.25f),
    outlineVariant = DeepBlueLight.copy(alpha = 0.10f),

    inverseSurface = DeepBlueDark,
    inverseOnSurface = OnDeepBlue,
    inversePrimary = DeepBlueLight,

    // Material3 surface containers at different elevations
    surfaceContainerLowest = SoftWhite,
    surfaceContainerLow = SurfaceWhite,
    surfaceContainer = SurfaceWhite,
    surfaceContainerHigh = DeepBlueSurface.copy(alpha = 0.4f),
    surfaceContainerHighest = DeepBlueSurface.copy(alpha = 0.7f),
)

// ═══════════════════════════════════════════
// Dark Color Scheme
// ═══════════════════════════════════════════
private val DarkColorScheme = darkColorScheme(
    primary = DeepBlueLighter,
    onPrimary = DeepBlueDarker,
    primaryContainer = DeepBlue,
    onPrimaryContainer = DeepBlueSurface,

    secondary = EmeraldGreenLighter,
    onSecondary = EmeraldGreenDarker,
    secondaryContainer = EmeraldGreen,
    onSecondaryContainer = EmeraldGreenSurface,

    tertiary = GoldLight,
    onTertiary = GoldDarker,
    tertiaryContainer = GoldDark,
    onTertiaryContainer = GoldSurface,

    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDarkElevated,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = OnSurfaceDarkDim,

    error = Color(0xFFFF6E6E),
    onError = Color(0xFF2D0000),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFCDD2),

    outline = DeepBlueLighter.copy(alpha = 0.35f),
    outlineVariant = DeepBlueLighter.copy(alpha = 0.12f),

    inverseSurface = SoftWhite,
    inverseOnSurface = OnSurface,
    inversePrimary = DeepBlue,

    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceDarkElevated,
    surfaceContainer = SurfaceDarkCard,
    surfaceContainerHigh = SurfaceDarkInput,
    surfaceContainerHighest = DeepBlueDark.copy(alpha = 0.6f),
)

// ═══════════════════════════════════════════
// Theme Composable
// ═══════════════════════════════════════════
@Composable
fun ElSuarKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Immersive edge-to-edge with transparent system bars
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }

            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// ═══════════════════════════════════════════
// Screen Transition Animations
// ═══════════════════════════════════════════

/**
 * Standard screen transition: slide from right + fade.
 */
@Composable
fun NavTransition(content: @Composable () -> Unit) {
    AnimatedContent(
        targetState = true,
        transitionSpec = {
            (fadeIn(animationSpec = tween(280, delayMillis = 30))
                + slideInHorizontally(animationSpec = tween(320)) { it / 4 })
                .togetherWith(
                    fadeOut(animationSpec = tween(200))
                        + slideOutHorizontally(animationSpec = tween(280)) { -it / 4 }
                )
        },
        label = "nav_transition"
    ) { content() }
}

/**
 * Quick fade-only transition for same-level navigation (e.g. tab switches).
 */
@Composable
fun FadeTransition(content: @Composable () -> Unit) {
    AnimatedContent(
        targetState = true,
        transitionSpec = {
            fadeIn(animationSpec = tween(200))
                .togetherWith(fadeOut(animationSpec = tween(150)))
        },
        label = "fade_transition"
    ) { content() }
}

/**
 * Scale-up entrance for dialogs and overlays.
 */
@Composable
fun ScaleUpTransition(content: @Composable () -> Unit) {
    AnimatedContent(
        targetState = true,
        transitionSpec = {
            (fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(240, delayMillis = 20), initialScale = 0.92f))
                .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(180), targetScale = 0.95f))
        },
        label = "scale_transition"
    ) { content() }
}
