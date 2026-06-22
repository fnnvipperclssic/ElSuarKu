package com.example.elsuarku.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.*

/**
 * Premium glassmorphism card with frosted-glass aesthetic.
 *
 * Features:
 *  - Multi-stop vertical gradient for realistic glass depth
 *  - Subtle border with configurable alpha
 *  - Layered shadow (ambient + spot) in brand blue
 *  - Dark/light mode aware
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    borderAlpha: Float = 0.25f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = if (isDark) GlassSurfaceDarkAlt else GlassSurfaceAlt
    val borderColor = if (isDark) GlassBorderDark else GlassBorderLight
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else DeepBlue.copy(alpha = 0.06f)
    val spotShadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else DeepBlue.copy(alpha = 0.10f)

    // Press-state animation for tactile feedback on glass surfaces
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glass_press_scale"
    )
    val pressBorderAlpha = if (isPressed) (borderAlpha * 1.6f).coerceAtMost(0.6f) else borderAlpha

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null, // we provide our own scale feedback — cleaner on glass
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(clickModifier)
            .scale(pressScale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = spotShadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.98f),
                        surfaceColor.copy(alpha = 0.88f),
                        surfaceColor.copy(alpha = 0.78f),
                        surfaceColor.copy(alpha = 0.88f),
                        surfaceColor.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = borderColor.copy(alpha = pressBorderAlpha),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp),
        content = content
    )
}

/**
 * Elevated GlassCard for featured/hero content — larger shadow & subtle gold accent.
 */
@Composable
fun GlassCardElevated(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = if (isDark) GlassSurfaceDarkAlt else GlassSurfaceAlt
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else DeepBlue.copy(alpha = 0.10f)
    val spotShadowColor = if (isDark) Color.Black.copy(alpha = 0.6f) else DeepBlue.copy(alpha = 0.15f)

    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = spotShadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.98f),
                        surfaceColor.copy(alpha = 0.85f),
                        surfaceColor.copy(alpha = 0.75f),
                        surfaceColor.copy(alpha = 0.88f),
                        surfaceColor.copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Gold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(20.dp),
        content = content
    )
}

/**
 * Animated shimmer loading placeholder card.
 * Smooth linear gradient sweep across 1200ms.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    height: Dp = 100.dp
) {
    val shimmerColors = listOf(
        DeepBlueSurface.copy(alpha = 0.3f),
        DeepBlueSurfaceVivid.copy(alpha = 0.7f),
        DeepBlueSurface.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(cornerRadius), ambientColor = DeepBlue.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim.value - 300f, 0f),
                    end = Offset(translateAnim.value + 100f, 0f)
                )
            )
            .height(height)
            .padding(16.dp)
    )
}

/**
 * Compact glass surface for inline chips, tags, small info blocks.
 */
@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val surfaceColor = if (isDark) GlassSurfaceDark.copy(alpha = 0.7f) else GlassSurface.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(0.5.dp, GlassBorderLight.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { content() }
}
