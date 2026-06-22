package com.example.elsuarku.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueLight

/**
 * Dashboard statistic card — shows a single metric with icon and label.
 *
 * @param title       Label below the value
 * @param value       Display string (e.g. "1,234")
 * @param icon        Material icon
 * @param accentColor Icon tint color
 * @param animateValue If true and value parses as Int, animates counting up
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = DeepBlueLight,
    modifier: Modifier = Modifier,
    animateValue: Boolean = false
) {
    // Try parsing the value for animation
    val numericValue = if (animateValue) {
        value.replace(Regex("[^0-9]"), "").toIntOrNull()
    } else null

    val animatedProgress by animateFloatAsState(
        targetValue = if (numericValue != null) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "stat_progress"
    )

    val displayValue = if (numericValue != null) {
        val current = (numericValue * animatedProgress).toInt()
        formatNumber(current)
    } else value

    GlassCard(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(28.dp),
            tint = accentColor
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = displayValue,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

/**
 * Formats a number with thousand separators (e.g. 1234 → "1,234").
 */
private fun formatNumber(n: Int): String {
    if (n == 0) return "0"
    val sb = StringBuilder()
    var remaining = n
    while (remaining > 0) {
        if (sb.isNotEmpty() && sb.length % 4 == 3) sb.insert(0, ',')
        sb.insert(0, remaining % 10)
        remaining /= 10
    }
    return sb.toString()
}
