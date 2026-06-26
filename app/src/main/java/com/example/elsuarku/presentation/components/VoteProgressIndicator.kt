package com.example.elsuarku.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.*

/**
 * Step-by-step vote submission progress indicator.
 *
 * Shows the user real-time feedback during the two-phase commit process:
 *   1. Encrypting  — encrypt vote data with AES-256-GCM
 *   2. Submitting  — writing to Firestore (atomic transaction)
 *   3. Confirming  — incrementing counters, audit log
 *   4. Complete    — all done, vote secured
 *
 * Each step transitions through: PENDING → ACTIVE → DONE
 * Failed steps show an error state.
 *
 * ## Usage
 * ```kotlin
 * VoteProgressIndicator(
 *     currentStep = voteStep,  // from VoteState
 *     stepStates = stepStates, // per-step status map
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 */

enum class VoteStep {
    ENCRYPTING,
    SUBMITTING,
    CONFIRMING,
    COMPLETE
}

enum class StepStatus { PENDING, ACTIVE, DONE, ERROR }

data class StepInfo(
    val step: VoteStep,
    val icon: ImageVector,
    val label: String,
    val description: String
)

@Composable
fun VoteProgressIndicator(
    currentStep: VoteStep,
    stepStates: Map<VoteStep, StepStatus>,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        StepInfo(VoteStep.ENCRYPTING, Icons.Filled.Lock, "Enkripsi", "Mengamankan data suara"),
        StepInfo(VoteStep.SUBMITTING, Icons.Filled.Send, "Kirim", "Menyimpan suara ke server"),
        StepInfo(VoteStep.CONFIRMING, Icons.Filled.Security, "Konfirmasi", "Memverifikasi integritas"),
        StepInfo(VoteStep.COMPLETE, Icons.Filled.Check, "Selesai", "Suara berhasil disimpan")
    )

    Column(modifier = modifier.padding(16.dp)) {
        steps.forEachIndexed { index, stepInfo ->
            val status = stepStates[stepInfo.step] ?: StepStatus.PENDING
            val isLast = index == steps.lastIndex

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Step circle + connector line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    StepCircle(status = status, icon = stepInfo.icon)
                    if (!isLast) {
                        StepConnector(
                            active = status == StepStatus.DONE,
                            modifier = Modifier.height(32.dp).width(2.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Step label + description
                Column(modifier = Modifier.weight(1f).padding(bottom = if (!isLast) 16.dp else 0.dp)) {
                    Text(
                        text = stepInfo.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (status == StepStatus.ACTIVE) FontWeight.Bold
                                       else FontWeight.Medium
                        ),
                        color = when (status) {
                            StepStatus.DONE -> EmeraldGreen
                            StepStatus.ACTIVE -> DeepBlueDark
                            StepStatus.ERROR -> StatusError
                            StepStatus.PENDING -> DeepBlueLight.copy(alpha = 0.5f)
                        }
                    )
                    AnimatedVisibility(visible = status == StepStatus.ACTIVE) {
                        Text(
                            text = stepInfo.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepBlueLight
                        )
                    }
                    if (status == StepStatus.ERROR) {
                        Text(
                            text = "Gagal — coba lagi",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCircle(status: StepStatus, icon: ImageVector) {
    val bgColor = when (status) {
        StepStatus.DONE -> EmeraldGreen
        StepStatus.ACTIVE -> DeepBlue
        StepStatus.ERROR -> StatusError
        StepStatus.PENDING -> DeepBlueLight.copy(alpha = 0.15f)
    }
    val iconColor = when (status) {
        StepStatus.DONE -> Color.White
        StepStatus.ACTIVE -> Color.White
        StepStatus.ERROR -> Color.White
        StepStatus.PENDING -> DeepBlueLight.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (status == StepStatus.ACTIVE) {
            // Pulsing dot for active step
            PulseDot(color = Color.White, size = 6)
        } else {
            Icon(
                imageVector = if (status == StepStatus.DONE) Icons.Filled.Check else icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StepConnector(active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                if (active) EmeraldGreen.copy(alpha = 0.5f)
                else DeepBlueLight.copy(alpha = 0.15f),
                RoundedCornerShape(1.dp)
            )
    )
}
