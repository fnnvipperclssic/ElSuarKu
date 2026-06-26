package com.example.elsuarku.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable navigation transition specs for Compose Navigation animated routes.
 *
 * Usage with NavHost:
 * ```
 * NavHost(
 *     enterTransition = { ScaleSlideEnter },
 *     exitTransition = { ScaleSlideExit },
 *     popEnterTransition = { PopEnterTransition },
 *     popExitTransition = { PopExitTransition }
 * )
 * ```
 */

// ---------------------------------------------------------------------------
// Shared animation parameters
// ---------------------------------------------------------------------------
private const val TRANSITION_DURATION = 350

// ---------------------------------------------------------------------------
// Forward transitions (navigate to new screen)
// ---------------------------------------------------------------------------

/** Slide right-to-left + slight fade — standard forward navigation */
val SlideFadeEnter: EnterTransition
    @Composable get() = slideInHorizontally(
        animationSpec = tween(TRANSITION_DURATION),
        initialOffsetX = { it / 4 }            // 25% from right
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))

val SlideFadeExit: ExitTransition
    @Composable get() = slideOutHorizontally(
        animationSpec = tween(TRANSITION_DURATION),
        targetOffsetX = { -it / 4 }            // 25% to left
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))

// ---------------------------------------------------------------------------
// Pop transitions (back navigation)
// ---------------------------------------------------------------------------

/** Slide left-to-right — standard back navigation */
val PopSlideEnter: EnterTransition
    @Composable get() = slideInHorizontally(
        animationSpec = tween(TRANSITION_DURATION),
        initialOffsetX = { -it / 4 }           // 25% from left
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))

val PopSlideExit: ExitTransition
    @Composable get() = slideOutHorizontally(
        animationSpec = tween(TRANSITION_DURATION),
        targetOffsetX = { it / 4 }             // 25% to right
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))

// ---------------------------------------------------------------------------
// Scale transitions (for dialogs/detail screens)
// ---------------------------------------------------------------------------

/** Scale up + fade in — modal/detail enter */
val ScaleSlideEnter: EnterTransition
    @Composable get() = scaleIn(
        animationSpec = tween(TRANSITION_DURATION),
        initialScale = 0.92f
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))

val ScaleSlideExit: ExitTransition
    @Composable get() = scaleOut(
        animationSpec = tween(TRANSITION_DURATION),
        targetScale = 0.92f
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))

// ---------------------------------------------------------------------------
// Utility — apply transitions to any content
// ---------------------------------------------------------------------------

/** Wraps content with enter/exit animation for AnimatedContent or manual use */
@Composable
fun AnimatedScreenTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = SlideFadeEnter,
        exit = SlideFadeExit,
        modifier = modifier
    ) {
        content()
    }
}
