package com.example.elsuarku.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium election card with status badge, progress bar, and info chips.
 */
@Composable
fun ElectionCard(
    election: Election,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (election.status) {
        ElectionStatus.ACTIVE -> EmeraldGreen
        ElectionStatus.DRAFT -> Gold
        ElectionStatus.COMPLETED -> DeepBlueLight
        ElectionStatus.CANCELLED -> StatusError
    }

    val statusLabel = when (election.status) {
        ElectionStatus.ACTIVE -> "AKTIF"
        ElectionStatus.DRAFT -> "DRAFT"
        ElectionStatus.COMPLETED -> "SELESAI"
        ElectionStatus.CANCELLED -> "DIBATALKAN"
    }

    // Participation ratio for progress bar
    val participation = if (election.totalVoters > 0) {
        (election.votedCount.toFloat() / election.totalVoters.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val progressColor = when {
        participation >= 0.75f -> EmeraldGreenLight
        participation >= 0.50f -> Gold
        else -> DeepBlueLight
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        // Header row: title + status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = election.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = election.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            StatusBadge(label = statusLabel, color = statusColor)
        }

        // Progress bar (only for active elections with voters)
        if (election.status == ElectionStatus.ACTIVE && election.totalVoters > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Partisipasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${(participation * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = progressColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { participation },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InfoChip(
                icon = Icons.Filled.People,
                text = "${election.votedCount}/${election.totalVoters} Pemilih"
            )
            InfoChip(
                icon = Icons.Filled.AccessTime,
                text = formatDateRange(election.startDate, election.endDate)
            )
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

private fun formatDateRange(start: Long, end: Long): String {
    val fmt = SimpleDateFormat("dd/MM/yy", Locale.forLanguageTag("id"))
    return "${fmt.format(Date(start))} - ${fmt.format(Date(end))}"
}
