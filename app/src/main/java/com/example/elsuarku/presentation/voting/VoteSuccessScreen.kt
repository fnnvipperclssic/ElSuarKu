package com.example.elsuarku.presentation.voting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.*

@Composable
fun VoteSuccessScreen(
    viewModel: VotingViewModel? = null,
    onBackToDashboard: () -> Unit
) {
    val voteState = viewModel?.voteState?.collectAsState()
    var phase1 by remember { mutableStateOf(false) }
    var phase2 by remember { mutableStateOf(false) }
    var phase3 by remember { mutableStateOf(false) }
    var phase4 by remember { mutableStateOf(false) }

    // Check icon — spring bounce
    val checkScale by animateFloatAsState(
        targetValue = if (phase1) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )

    // Glow ring scale
    val glowScale by animateFloatAsState(
        targetValue = if (phase1) 1.4f else 0.5f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "glowScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (phase1) 0f else 0.3f,
        animationSpec = tween(1000),
        label = "glowAlpha"
    )

    // Text slide up
    val textOffset by animateDpAsState(
        targetValue = if (phase2) 0.dp else 40.dp,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "textOffset"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (phase2) 1f else 0f,
        animationSpec = tween(500),
        label = "textAlpha"
    )

    // Badge slide up
    val badgeOffset by animateDpAsState(
        targetValue = if (phase3) 0.dp else 30.dp,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "badgeOffset"
    )

    // Button slide up
    val btnOffset by animateDpAsState(
        targetValue = if (phase4) 0.dp else 60.dp,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "btnOffset"
    )

    // Block system back button from returning to VoteConfirmation
    BackHandler(enabled = true) {
        onBackToDashboard()
    }

    LaunchedEffect(Unit) {
        phase1 = true
        kotlinx.coroutines.delay(500)
        phase2 = true
        kotlinx.coroutines.delay(400)
        phase3 = true
        kotlinx.coroutines.delay(300)
        phase4 = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepBlueDarker, DeepBlueDark, DeepBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Check icon with glow ring
            Box(contentAlignment = Alignment.Center) {
                // Glow ring behind check
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(glowAlpha)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EmeraldGreen.copy(alpha = 0.3f),
                                    EmeraldGreen.copy(alpha = 0f)
                                )
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Sukses",
                    modifier = Modifier
                        .size(100.dp)
                        .scale(checkScale),
                    tint = EmeraldGreen
                )
            }

            Spacer(Modifier.height(28.dp))

            // Title + description
            Column(
                modifier = Modifier
                    .offset(y = textOffset)
                    .alpha(textAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Suara Terkirim!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnDeepBlue,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                // Show who/what the user voted for
                voteState?.value?.candidate?.let { candidate ->
                    Text(
                        "Anda memilih:",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnDeepBlue.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        candidate.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Gold,
                        textAlign = TextAlign.Center
                    )
                    voteState?.value?.election?.let { election ->
                        Text(
                            "untuk pemilihan: ${election.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnDeepBlue.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } ?: run {
                    Text(
                        "Suara Anda telah tercatat dengan aman\ndan terenkripsi. Terima kasih telah\nberpartisipasi!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnDeepBlue.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Security badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset(y = badgeOffset)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Gold.copy(alpha = 0.12f))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.Shield, null, tint = Gold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Dilindungi AES-256-GCM",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Gold
                    )
                    Text(
                        "Suara Anda tidak dapat diubah",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold.copy(alpha = 0.6f)
                    )
                }
            }

            // Biometric verification badge
            if (voteState?.value?.biometricVerified == true) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Fingerprint, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Terverifikasi Biometrik",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = EmeraldGreen
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Back button
            Button(
                onClick = onBackToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .offset(y = btnOffset),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Kembali ke Dashboard",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
        }
    }
}
