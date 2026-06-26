package com.example.elsuarku.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.DeepBlueLight

/**
 * Shimmer loading placeholder — mimics content layout while data loads.
 *
 * Used for skeleton screens during initial data fetch, replacing
 * static loading indicators with an animated placeholder that matches
 * the expected content layout.
 */

@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    val shimmerColors = shimmerBrush()

    Surface(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColors)
            )
            Spacer(Modifier.height(8.dp))
            // Subtitle placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColors)
            )
            Spacer(Modifier.height(12.dp))
            // Avatar + text row
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(shimmerColors)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColors)
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColors)
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerList(itemCount: Int = 5) {
    Column {
        repeat(itemCount) {
            ShimmerCard()
        }
    }
}

@Composable
fun ShimmerDetail(modifier: Modifier = Modifier) {
    val shimmerColors = shimmerBrush()

    Column(modifier = modifier.padding(16.dp)) {
        // Hero image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmerColors)
        )
        Spacer(Modifier.height(20.dp))
        // Title
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerColors)
        )
        Spacer(Modifier.height(12.dp))
        // Description lines
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (it == 3) 0.5f else 1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColors)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Creates an animated shimmer brush for skeleton loading.
 * The gradient sweeps from left to right, simulating a light reflection.
 */
@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        DeepBlueLight.copy(alpha = 0.1f),
        DeepBlueLight.copy(alpha = 0.3f),
        DeepBlueLight.copy(alpha = 0.1f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 200f, translateAnimation.value - 200f),
        end = Offset(translateAnimation.value, translateAnimation.value)
    )
}
