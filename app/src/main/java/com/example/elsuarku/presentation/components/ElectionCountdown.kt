package com.example.elsuarku.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Live countdown timer for election periods.
 *
 * Shows different states:
 *  - NOT_STARTED: Days/hours until election opens
 *  - ACTIVE: Days/hours remaining before election closes
 *  - CLOSING_SOON: < 1 hour remaining — red alert
 *  - ENDED: Election closed
 *
 * Updates every second via LaunchedEffect timer.
 *
 * @param startDate Election start timestamp (epoch millis)
 * @param endDate Election end timestamp (epoch millis)
 */
@Composable
fun ElectionCountdown(
    startDate: Long,
    endDate: Long,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val state = remember(startDate, endDate, now) {
        when {
            now > endDate -> CountdownState.ENDED
            now in startDate..endDate -> {
                if (endDate - now < 3_600_000L) CountdownState.CLOSING_SOON
                else CountdownState.ACTIVE
            }
            else -> CountdownState.NOT_STARTED
        }
    }

    val remaining = remember(startDate, endDate, now, state) {
        when (state) {
            CountdownState.NOT_STARTED -> startDate - now
            CountdownState.ACTIVE, CountdownState.CLOSING_SOON -> endDate - now
            CountdownState.ENDED -> 0L
        }
    }

    val days = remaining / 86_400_000L
    val hours = (remaining % 86_400_000L) / 3_600_000L
    val minutes = (remaining % 3_600_000L) / 60_000L
    val seconds = (remaining % 60_000L) / 1000L

    val bgColor = when (state) {
        CountdownState.CLOSING_SOON -> StatusError.copy(alpha = 0.1f)
        CountdownState.ACTIVE -> EmeraldGreen.copy(alpha = 0.1f)
        CountdownState.NOT_STARTED -> StatusWarning.copy(alpha = 0.1f)
        CountdownState.ENDED -> DeepBlueLight.copy(alpha = 0.1f)
    }

    val accentColor = when (state) {
        CountdownState.CLOSING_SOON -> StatusError
        CountdownState.ACTIVE -> EmeraldGreen
        CountdownState.NOT_STARTED -> StatusWarning
        CountdownState.ENDED -> DeepBlueLight
    }

    val icon = when (state) {
        CountdownState.CLOSING_SOON -> Icons.Filled.HourglassBottom
        CountdownState.ACTIVE -> Icons.Filled.AccessTime
        CountdownState.NOT_STARTED -> Icons.Filled.AccessTime
        CountdownState.ENDED -> Icons.Filled.HourglassBottom
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (state) {
                        CountdownState.NOT_STARTED -> "Pemilihan dimulai dalam"
                        CountdownState.ACTIVE -> "Pemilihan berakhir dalam"
                        CountdownState.CLOSING_SOON -> "SEGERA BERAKHIR!"
                        CountdownState.ENDED -> "Pemilihan telah berakhir"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor
                )
            }

            if (state != CountdownState.ENDED) {
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    CountdownUnit(days, "Hari", accentColor)
                    CountdownSeparator(accentColor)
                    CountdownUnit(hours, "Jam", accentColor)
                    CountdownSeparator(accentColor)
                    CountdownUnit(minutes, "Menit", accentColor)
                    CountdownSeparator(accentColor)
                    CountdownUnit(seconds, "Detik", accentColor)
                }
            }
        }
    }
}

@Composable
private fun CountdownUnit(value: Long, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(false) },
            label = "countdown-$label"
        ) { v ->
            Text(
                text = "%02d".format(v),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CountdownSeparator(color: androidx.compose.ui.graphics.Color) {
    Text(
        text = ":",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = color.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 14.dp)
    )
}

enum class CountdownState {
    NOT_STARTED,
    ACTIVE,
    CLOSING_SOON,
    ENDED
}
