package com.example.elsuarku.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.ui.theme.*

/**
 * Bar chart card showing vote counts per candidate.
 *
 * Pure Compose Canvas implementation — no external chart library.
 *
 * @param data List of (candidateName, voteCount) pairs
 * @param title Chart title
 */
@Composable
fun BarChartCard(
    data: List<Pair<String, Int>>,
    title: String = "Hasil Suara",
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
                    "Belum ada data suara",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBlueLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val maxVotes = data.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f

                // Y-axis label
                Text(
                    "$maxVotes suara",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepBlueLight
                )

                val barColors = listOf(
                    DeepBlue, EmeraldGreen, StatusWarning,
                    StatusError, Color(0xFF6200EE), Color(0xFF03DAC5),
                    Color(0xFFE91E63), Color(0xFFFF9800)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 8.dp, bottom = 24.dp)
                ) {
                    val barCount = data.size
                    if (barCount == 0) return@Canvas
                    val gapRatio = 0.3f
                    val totalGaps = barCount + 1
                    val barWidth = (size.width * (1 - gapRatio)) / barCount
                    val gapWidth = (size.width * gapRatio) / totalGaps

                    data.forEachIndexed { index, (_, count) ->
                        val barHeight = (count.toFloat() / maxVotes) * size.height
                        val x = gapWidth + index * (barWidth + gapWidth)
                        val y = size.height - barHeight

                        drawRoundRect(
                            color = barColors[index % barColors.size],
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }

                // X-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.forEach { (name, _) ->
                        Text(
                            text = name.take(10),
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepBlueLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Legend with values
                data.forEachIndexed { index, (name, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(color = barColors[index % barColors.size])
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepBlueDark
                            )
                        }
                        Text(
                            text = "$count suara",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = barColors[index % barColors.size]
                        )
                    }
                }
            }
        }
    }
}
