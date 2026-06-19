package com.example.elsuarku.presentation.voting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue

@Composable
fun VoteSuccessScreen(
    onBackToDashboard: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlueDark),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn() + fadeIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // Success icon
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Sukses",
                    modifier = Modifier.size(96.dp),
                    tint = EmeraldGreen
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Suara Terkirim!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnDeepBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Suara Anda telah tercatat dengan aman dan terenkripsi. " +
                            "Terima kasih telah berpartisipasi dalam pemilihan ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnDeepBlue.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Security shield icon
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Aman",
                    modifier = Modifier.size(48.dp),
                    tint = Gold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Dilindungi dengan Enkripsi AES-256-GCM",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onBackToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Kembali ke Dashboard",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepBlueDark
                    )
                }
            }
        }
    }
}
