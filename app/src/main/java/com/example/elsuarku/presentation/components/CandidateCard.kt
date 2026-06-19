package com.example.elsuarku.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.GoldDark

/**
 * Card for displaying a candidate in the voting list.
 */
@Composable
fun CandidateCard(
    candidate: Candidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(candidate.photoBase64) {
        if (candidate.photoBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(candidate.photoBase64, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nomor Urut
            NumberBadge(number = candidate.nomorUrut)

            Spacer(modifier = Modifier.width(16.dp))

            // Photo or placeholder
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = candidate.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                PlaceholderAvatar(name = candidate.name)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (candidate.visi.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = candidate.visi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Filled.Person, // Using Person as arrow indicator
                contentDescription = "Lihat detail",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun NumberBadge(number: Int) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (number == 1) Gold
                else if (number == 2) DeepBlueLight
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = if (number <= 2) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PlaceholderAvatar(name: String) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(DeepBlueLight.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineMedium,
            color = DeepBlueLight
        )
    }
}
