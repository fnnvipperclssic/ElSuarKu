package com.example.elsuarku.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueLight

/**
 * Reusable full-screen empty state.
 *
 * Consistent across all screens — shows icon, title, subtitle, and optional action button.
 *
 * @param icon The icon to display (default: inbox)
 * @param title Primary message (e.g., "Belum Ada Pemilihan")
 * @param subtitle Secondary description (e.g., "Pemilihan akan muncul di sini setelah admin membuatnya")
 * @param actionLabel Optional action button label
 * @param onAction Optional action callback
 */
@Composable
fun EmptyState(
    icon: ImageVector = Icons.Filled.Inbox,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepBlueLight.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DeepBlueDark,
                    textAlign = TextAlign.Center
                )

                if (subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueLight,
                        textAlign = TextAlign.Center
                    )
                }

                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

/**
 * Pre-configured empty states for common ElSuarKu scenarios.
 */
object EmptyStates {

    @Composable
    fun NoElections(onCreateElection: (() -> Unit)? = null) {
        EmptyState(
            icon = Icons.Outlined.HowToVote,
            title = "Belum Ada Pemilihan",
            subtitle = "Pemilihan akan muncul di sini setelah admin membuatnya.",
            actionLabel = if (onCreateElection != null) "Buat Pemilihan" else null,
            onAction = onCreateElection
        )
    }

    @Composable
    fun NoCandidates() {
        EmptyState(
            icon = Icons.Outlined.People,
            title = "Belum Ada Kandidat",
            subtitle = "Kandidat akan muncul di sini setelah admin menambahkannya."
        )
    }

    @Composable
    fun NoVotes() {
        EmptyState(
            icon = Icons.Outlined.Person,
            title = "Belum Ada Suara",
            subtitle = "Belum ada pemilih yang memberikan suara dalam pemilihan ini."
        )
    }

    @Composable
    fun NoUsers() {
        EmptyState(
            icon = Icons.Outlined.People,
            title = "Belum Ada Pengguna",
            subtitle = "Pengguna akan muncul di sini setelah mereka mendaftar."
        )
    }
}
