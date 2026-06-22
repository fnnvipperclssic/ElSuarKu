package com.example.elsuarku.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.*

enum class SecurityLevel {
    SECURE,      // Green — system secure
    WARNING,     // Yellow — attention needed
    ALERT,       // Red — security threat
    VERIFIED     // Gold — identity verified
}

/**
 * Security status badge with pulse animation for WARNING and ALERT levels.
 */
@Composable
fun SecurityBadge(
    level: SecurityLevel,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val (bgColor, icon, label) = when (level) {
        SecurityLevel.SECURE -> Triple(
            EmeraldGreenSurface,
            Icons.Filled.Shield,
            "Aman"
        )
        SecurityLevel.WARNING -> Triple(
            StatusWarningSurface,
            Icons.Filled.Security,
            "Perhatian"
        )
        SecurityLevel.ALERT -> Triple(
            StatusErrorSurface,
            Icons.Filled.Security,
            "Waspada"
        )
        SecurityLevel.VERIFIED -> Triple(
            GoldSurface,
            Icons.Filled.VerifiedUser,
            "Terverifikasi"
        )
    }

    val iconColor = when (level) {
        SecurityLevel.SECURE -> EmeraldGreen
        SecurityLevel.WARNING -> StatusWarning
        SecurityLevel.ALERT -> StatusError
        SecurityLevel.VERIFIED -> Gold
    }

    // Pulse animation for WARNING and ALERT levels
    val shouldPulse = animated && (level == SecurityLevel.WARNING || level == SecurityLevel.ALERT)
    val pulseAlpha: Float = if (shouldPulse) {
        val transition = rememberInfiniteTransition(label = "pulse_${level.name}")
        val alpha by transition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        alpha
    } else {
        1f
    }

    Row(
        modifier = modifier
            .alpha(if (shouldPulse) pulseAlpha else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Animated dot indicator for alert levels
        if (level == SecurityLevel.ALERT) {
            PulseDot(color = StatusError)
        }
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor
        )
    }
}

/**
 * Tiny pulsing dot indicator for real-time status.
 */
@Composable
fun PulseDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 6
) {
    val transition = rememberInfiniteTransition(label = "dot_pulse")
    val scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .scale(scale)
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}
