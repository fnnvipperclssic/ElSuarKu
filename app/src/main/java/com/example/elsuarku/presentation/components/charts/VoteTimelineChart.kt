package com.example.elsuarku.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.ui.theme.*

/**
 * Line chart showing voter turnout over time (votes per hour).
 *
 * Pure Compose Canvas implementation — no external chart library needed.
 * Data points are connected with smooth curves, and a fill gradient
 * shows the area under the line.
 *
 * @param data List of (hourLabel, voteCount) pairs ordered chronologically
 * @param title Chart title
 */
@Composable
fun VoteTimelineChart(
    data: List<Pair<String, Int>>,
    title: String = "Tren Partisipasi",
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

            if (data.size < 2) {
                Text(
                    "Data belum cukup untuk menampilkan tren",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBlueLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val maxVal = data.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f

                // Y-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "$maxVal suara",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepBlueLight
                    )
                    Text(
                        "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepBlueLight
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (data.size - 1).coerceAtLeast(1)

                    // Build path and fill area
                    val linePath = Path()
                    val fillPath = Path()

                    data.forEachIndexed { index, (_, count) ->
                        val x = index * stepX
                        val y = height - (count.toFloat() / maxVal * height)

                        if (index == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            // Smooth curve using control points
                            val prevX = (index - 1) * stepX
                            val prevY = height - (data[index - 1].second.toFloat() / maxVal * height)
                            val cpX = (prevX + x) / 2
                            linePath.cubicTo(cpX, prevY, cpX, y, x, y)
                            fillPath.cubicTo(cpX, prevY, cpX, y, x, y)
                        }
                    }

                    // Close fill path
                    val lastX = (data.size - 1) * stepX
                    fillPath.lineTo(lastX, height)
                    fillPath.close()

                    // Fill area under curve
                    drawPath(
                        path = fillPath,
                        color = DeepBlue.copy(alpha = 0.08f)
                    )

                    // Draw the line
                    drawPath(
                        path = linePath,
                        color = DeepBlue,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw data points
                    data.forEachIndexed { index, (_, count) ->
                        val x = index * stepX
                        val y = height - (count.toFloat() / maxVal * height)
                        // Outer ring
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        // Inner dot
                        drawCircle(
                            color = DeepBlue,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // X-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEachIndexed { index, (label, _) ->
                        if (index == 0 || index == data.lastIndex || index % (data.size / 4.coerceAtLeast(1)) == 0) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepBlueLight,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                    }
                }
            }
        }
    }
}
