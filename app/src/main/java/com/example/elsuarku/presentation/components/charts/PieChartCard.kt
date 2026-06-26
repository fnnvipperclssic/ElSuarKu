package com.example.elsuarku.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.ui.theme.*

/**
 * Donut chart showing candidate vote distribution.
 *
 * Pure Compose Canvas implementation — no external chart library needed for donut.
 * Colors cycle through the ElSuarKu palette automatically.
 *
 * @param data List of (candidateName, voteCount) pairs
 * @param title Chart title
 */
@Composable
fun PieChartCard(
    data: List<Pair<String, Int>>,
    title: String = "Distribusi Suara",
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DeepBlueDark
            )

            Spacer(Modifier.height(16.dp))

            if (data.isEmpty() || data.all { it.second == 0 }) {
                Text(
                    "Belum ada suara masuk",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBlueLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val total = data.sumOf { it.second }
                val colors = chartColors.take(data.size)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut chart
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            slices = data.mapIndexed { i, (_, count) ->
                                DonutSlice(
                                    value = count.toFloat(),
                                    color = colors[i],
                                    percentage = if (total > 0) (count.toFloat() / total * 100f) else 0f
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Center text: total votes
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$total",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = DeepBlueDark
                            )
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepBlueLight
                            )
                        }
                    }

                    Spacer(Modifier.width(24.dp))

                    // Legend
                    Column(Modifier.weight(1f)) {
                        data.forEachIndexed { i, (name, count) ->
                            val pct = if (total > 0) "%.1f%%".format(count.toFloat() / total * 100f) else "0%"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(modifier = Modifier.size(10.dp)) {
                                    drawCircle(color = colors[i])
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = name.take(16),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepBlueDark,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count ($pct)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors[i]
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DonutSlice(
    val value: Float,
    val color: Color,
    val percentage: Float
)

@Composable
private fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val strokeWidth = 28f

    Canvas(modifier = modifier) {
        val padding = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(padding, padding)

        var startAngle = -90f

        slices.forEach { slice ->
            val sweep = if (total > 0) (slice.value / total) * 360f else 0f

            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

// Extended palette colors — must be defined BEFORE chartColors
private val Purple500 = Color(0xFF6200EE)
private val Teal200 = Color(0xFF03DAC5)
private val Pink500 = Color(0xFFE91E63)
private val Orange500 = Color(0xFFFF9800)

private val chartColors = listOf(
    DeepBlue,
    EmeraldGreen,
    StatusWarning,
    StatusError,
    Purple500,
    Teal200,
    Pink500,
    Orange500
)
