package com.example.elsuarku.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.*

/**
 * Premium loading indicator with animated three-dot pulse and optional label.
 */
@Composable
fun LoadingIndicator(message: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Three animated dots with staggered delays
            LoadingDot(delayMs = 0)
            LoadingDot(delayMs = 150)
            LoadingDot(delayMs = 300)
        }

        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

/**
 * Single animated dot for the loading indicator.
 */
@Composable
private fun LoadingDot(delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "dot_$delayMs")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs)
        ),
        label = "dot_alpha_$delayMs"
    )
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs)
        ),
        label = "dot_scale_$delayMs"
    )

    Box(
        modifier = Modifier
            .alpha(alpha)
            .size((10 * scale).dp)
            .clip(CircleShape)
            .background(Gold)
    )
}

/**
 * Full-page loading state with centered indicator on brand background.
 */
@Composable
fun FullPageLoader(message: String = "Memuat...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepBlueDarker,
                        DeepBlueDark,
                        DeepBlue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator()
            Spacer(Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnDeepBlue.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Skeleton loading placeholder for card content.
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 20.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp
) {
    val shimmerColors = listOf(
        DeepBlueLight.copy(alpha = 0.06f),
        DeepBlueLight.copy(alpha = 0.15f),
        DeepBlueLight.copy(alpha = 0.06f)
    )

    val transition = rememberInfiniteTransition(label = "skeleton")
    val anim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton_anim"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(anim - 300f, 0f),
                    end = Offset(anim + 100f, 0f)
                )
            )
            .height(height)
    )
}
