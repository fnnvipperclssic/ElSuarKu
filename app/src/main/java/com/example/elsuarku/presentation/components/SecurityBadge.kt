package com.example.elsuarku.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.EmeraldGreenSurface
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.GoldSurface
import com.example.elsuarku.ui.theme.StatusError
import com.example.elsuarku.ui.theme.StatusWarning
import com.example.elsuarku.ui.theme.StatusSuccess

enum class SecurityLevel {
    SECURE,      // Green — system secure
    WARNING,     // Yellow — attention needed
    ALERT,       // Red — security threat
    VERIFIED     // Gold — identity verified
}

@Composable
fun SecurityBadge(
    level: SecurityLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, icon, label) = when (level) {
        SecurityLevel.SECURE -> Triple(
            EmeraldGreenSurface,
            Icons.Filled.Shield,
            "Aman"
        )
        SecurityLevel.WARNING -> Triple(
            GoldSurface,
            Icons.Filled.Security,
            "Perhatian"
        )
        SecurityLevel.ALERT -> Triple(
            StatusError.copy(alpha = 0.1f),
            Icons.Filled.Security,
            "Waspada"
        )
        SecurityLevel.VERIFIED -> Triple(
            GoldSurface,
            Icons.Filled.VerifiedUser,
            "Terverifikasi"
        )
    }

    val iconColor = when (level) {
        SecurityLevel.SECURE -> EmeraldGreen
        SecurityLevel.WARNING -> StatusWarning
        SecurityLevel.ALERT -> StatusError
        SecurityLevel.VERIFIED -> Gold
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor
        )
    }
}
