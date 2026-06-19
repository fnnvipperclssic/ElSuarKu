package com.example.elsuarku.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.GlassBorder
import com.example.elsuarku.ui.theme.GlassBorderDark
import com.example.elsuarku.ui.theme.GlassSurface
import com.example.elsuarku.ui.theme.GlassSurfaceDark

/**
 * Glassmorphism card — frosted glass effect.
 * Used for dashboard widgets, election cards, and candidate profiles.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    cornerRadius: Int = 16,
    borderAlpha: Float = 0.3f,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceColor = if (isDark) GlassSurfaceDark else GlassSurface
    val borderColor = if (isDark) GlassBorderDark else GlassBorder

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.9f),
                        surfaceColor.copy(alpha = 0.7f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .padding(16.dp),
        content = content
    )
}
