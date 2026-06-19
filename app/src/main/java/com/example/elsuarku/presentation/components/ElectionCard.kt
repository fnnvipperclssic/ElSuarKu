package com.example.elsuarku.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.StatusError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card for displaying an election in a list.
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

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = election.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = election.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status badge
            StatusBadge(
                label = statusLabel,
                color = statusColor
            )
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
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun formatDateRange(start: Long, end: Long): String {
    val fmt = SimpleDateFormat("dd/MM/yy", Locale("id"))
    return "${fmt.format(Date(start))} - ${fmt.format(Date(end))}"
}
