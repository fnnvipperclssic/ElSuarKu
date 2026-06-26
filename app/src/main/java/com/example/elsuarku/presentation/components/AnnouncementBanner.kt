package com.example.elsuarku.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Announcement
import com.example.elsuarku.data.model.AnnouncementPriority
import com.example.elsuarku.ui.theme.*

/**
 * In-app announcement banner displayed at the top of the dashboard.
 *
 * Shows admin announcements in a swipeable card format, dismissible
 * by the user and automatically marked as seen.
 *
 * @param announcements List of active announcements to display
 * @param onDismiss Called when user dismisses an announcement
 */
@Composable
fun AnnouncementBanner(
    announcements: List<Announcement>,
    onDismiss: (Announcement) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (announcements.isEmpty()) return

    // Show only the most recent un-dismissed
    val current = announcements.firstOrNull() ?: return
    var visible by remember(current.id) { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        val priority = AnnouncementPriority.fromString(current.priority)
        val bgColor = when (priority) {
            AnnouncementPriority.HIGH -> StatusError.copy(alpha = 0.12f)
            AnnouncementPriority.NORMAL -> StatusWarning.copy(alpha = 0.12f)
            else -> DeepBlueLighter.copy(alpha = 0.12f)
        }
        val accentColor = when (priority) {
            AnnouncementPriority.HIGH -> StatusError
            AnnouncementPriority.NORMAL -> StatusWarning
            else -> DeepBlueLight
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (priority) {
                                AnnouncementPriority.HIGH -> "PENTING"
                                AnnouncementPriority.NORMAL -> "INFO"
                                else -> "UPDATE"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepBlueDark
                    )
                }
                if (current.message.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueDark.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = {
                    visible = false
                    onDismiss(current)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Tutup",
                    tint = DeepBlueLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

