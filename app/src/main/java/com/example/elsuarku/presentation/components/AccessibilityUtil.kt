package com.example.elsuarku.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.BuildCompat
import android.os.Build

/**
 * Accessibility utilities for ElSuarKu.
 *
 * Provides:
 *  - Minimum 48dp touch target enforcement
 *  - Heading semantics for TalkBack
 *  - Font scale detection and adaptation
 *  - Reduced motion preference detection
 *  - Color contrast verification helpers
 */

// ---------------------------------------------------------------------------
// Minimum touch target (WCAG 2.5.5: Target Size — 48dp)
// ---------------------------------------------------------------------------

/** Minimum recommended touch target size per WCAG 2.5.5 */
val MinTouchTarget: Dp = 48.dp

/**
 * Ensures a composable has at least 48dp minimum size for accessibility.
 * Applies `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)`.
 */
fun Modifier.minimumTouchTarget(): Modifier = this.then(
    Modifier.sizeIn(
        minWidth = MinTouchTarget,
        minHeight = MinTouchTarget
    )
)

// ---------------------------------------------------------------------------
// Heading semantics for TalkBack
// ---------------------------------------------------------------------------

/**
 * Marks a composable as a heading for screen reader navigation.
 * TalkBack users can navigate by headings to quickly scan screen structure.
 */
fun Modifier.headingSemantics(): Modifier = this.semantics {
    heading()
}

/**
 * Marks a composable as a clickable element (button/link semantics).
 */
fun Modifier.clickableSemantics(label: String): Modifier = this.semantics {
    contentDescription = label
    role = Role.Button
}

// ---------------------------------------------------------------------------
// Font scale & large text detection
// ---------------------------------------------------------------------------

/** Returns true if the user has enabled large font scaling (>= 1.5x) */
@Composable
fun isLargeFontScale(): Boolean {
    val config = LocalConfiguration.current
    return config.fontScale >= 1.5f
}

/** Returns true if the user has enabled any font scaling (> 1.0) */
@Composable
fun isFontScaled(): Boolean {
    return LocalConfiguration.current.fontScale > 1.0f
}

/**
 * Returns a layout configuration adapted to the current font scale.
 * Use to adjust spacing, sizing, or layout direction for large text.
 */
@Composable
fun accessibilityConfig(): AccessibilityConfig {
    val config = LocalConfiguration.current
    return AccessibilityConfig(
        fontScale = config.fontScale,
        isLargeText = config.fontScale >= 1.5f,
        isScreenReaderActive = isScreenReaderActive(),
        isReducedMotion = isReducedMotion()
    )
}

data class AccessibilityConfig(
    val fontScale: Float,
    val isLargeText: Boolean,
    val isScreenReaderActive: Boolean,
    val isReducedMotion: Boolean
)

// ---------------------------------------------------------------------------
// Screen reader detection
// ---------------------------------------------------------------------------

/**
 * Checks if a screen reader (TalkBack) is currently active.
 * Falls back to false if accessibility manager is unavailable.
 */
@Composable
fun isScreenReaderActive(): Boolean {
    val context = LocalContext.current
    return try {
        val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
                as? android.view.accessibility.AccessibilityManager
        am?.isEnabled == true && am.isTouchExplorationEnabled
    } catch (e: Exception) {
        false
    }
}

// ---------------------------------------------------------------------------
// Reduced motion preference
// ---------------------------------------------------------------------------

/**
 * Checks if user prefers reduced motion (Settings → Accessibility → Remove animations).
 * When true, disable non-essential animations (shimmer, parallax, complex transitions).
 */
@Composable
fun isReducedMotion(): Boolean {
    val context = LocalContext.current
    return try {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (e: Exception) {
        false
    }
}

// ---------------------------------------------------------------------------
// Color contrast helpers
// ---------------------------------------------------------------------------

/**
 * Relative luminance calculation per WCAG 2.1 definition.
 * Used to compute contrast ratio between two colors.
 *
 * @param red 0-255
 * @param green 0-255
 * @param blue 0-255
 * @return Relative luminance (0.0 - 1.0)
 */
fun relativeLuminance(red: Int, green: Int, blue: Int): Double {
    fun channel(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}

/**
 * WCAG 2.1 contrast ratio between two colors.
 * Returns ratio as Double (e.g., 4.5 = AA normal text, 3.0 = AA large text).
 */
fun contrastRatio(
    fgRed: Int, fgGreen: Int, fgBlue: Int,
    bgRed: Int, bgGreen: Int, bgBlue: Int
): Double {
    val l1 = relativeLuminance(fgRed, fgGreen, fgBlue)
    val l2 = relativeLuminance(bgRed, bgGreen, bgBlue)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Returns true if text colors meet WCAG AA minimum (4.5:1 for normal text).
 */
fun meetsWcagAA(fgRed: Int, fgGreen: Int, fgBlue: Int,
                bgRed: Int, bgGreen: Int, bgBlue: Int): Boolean {
    return contrastRatio(fgRed, fgGreen, fgBlue, bgRed, bgGreen, bgBlue) >= 4.5
}
