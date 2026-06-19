package com.example.elsuarku.presentation.voting

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.elsuarku.presentation.components.ConfirmationDialog
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.SecurityBadge
import com.example.elsuarku.presentation.components.SecurityLevel
import com.example.elsuarku.security.AntiTampering
import com.example.elsuarku.security.BiometricPromptManager
import com.example.elsuarku.security.BiometricResult
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueSurface
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue
import com.example.elsuarku.ui.theme.StatusError
import com.example.elsuarku.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteConfirmationScreen(
    electionId: String,
    candidateId: String,
    viewModel: VotingViewModel,
    onVoteSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.voteState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showIntegrityWarning by remember { mutableStateOf(false) }
    var integrityMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    val biometricManager = remember { BiometricPromptManager(context) }
    val antiTampering = remember { AntiTampering(context) }
    val biometricStatus = remember { biometricManager.canAuthenticate() }

    LaunchedEffect(electionId, candidateId) {
        viewModel.loadCandidateForVote(electionId, candidateId)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onVoteSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfirmasi Pilihan", color = OnDeepBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Security verification indicator
            SecurityBadge(level = SecurityLevel.VERIFIED)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Anda akan memilih:",
                style = MaterialTheme.typography.bodyLarge,
                color = DeepBlueDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Candidate info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepBlueSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Candidate photo or avatar
                    val bitmap = remember(state.candidate?.photoBase64) {
                        state.candidate?.photoBase64?.let {
                            try {
                                val bytes = Base64.decode(it, Base64.NO_WRAP)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Exception) { null }
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = state.candidate?.name,
                            modifier = Modifier.size(100.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Gold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = state.candidate?.name ?: "",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlue,
                        textAlign = TextAlign.Center
                    )

                    if ((state.candidate?.nomorUrut ?: 0) > 0) {
                        Text(
                            text = "Nomor Urut: ${state.candidate?.nomorUrut}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Biometric status indicator
            BiometricStatusIndicator(biometricStatus)

            Spacer(modifier = Modifier.height(12.dp))

            // Warning text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Pilihan bersifat permanen dan tidak dapat diubah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusWarning,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm button
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSubmitting) {
                    LoadingIndicator()
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Konfirmasi Pilihan Saya", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // ---- Confirmation Dialog → Biometric → Submit ----
    if (showConfirmDialog) {
        ConfirmationDialog(
            title = "Verifikasi Identitas",
            message = "Apakah Anda yakin ingin memberikan suara kepada ${state.candidate?.name}? " +
                    "Tindakan ini memerlukan verifikasi biometrik dan tidak dapat dibatalkan.",
            confirmText = "Ya, Pilih Sekarang",
            onConfirm = {
                showConfirmDialog = false

                // Run device integrity check
                val integrity = antiTampering.performFullCheck()
                if (!integrity.isSafe) {
                    showIntegrityWarning = true
                    integrityMessage = "Masalah keamanan terdeteksi: ${integrity.threats.joinToString(", ")}. " +
                            "Voting tidak dapat dilanjutkan demi keamanan."
                    return@ConfirmationDialog
                }

                // Try biometric verification
                val activity = context as? FragmentActivity
                if (biometricStatus.isAvailable && activity != null) {
                    biometricManager.authenticate(
                        activity = activity,
                        title = "Verifikasi Biometrik",
                        subtitle = "Konfirmasikan identitas Anda untuk memberikan suara",
                        onSuccess = {
                            viewModel.submitVote(electionId, candidateId)
                        },
                        onError = { error ->
                            integrityMessage = error
                            showIntegrityWarning = true
                        },
                        onCancel = {
                            // User cancelled — do nothing
                        }
                    )
                } else {
                    // No biometric available — warn but allow vote
                    integrityMessage = "Biometrik tidak tersedia (${biometricStatus.userFriendlyMessage}). " +
                            "Melanjutkan tanpa verifikasi biometrik."
                    showIntegrityWarning = true
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    // ---- Integrity / Biometric Warning ----
    if (showIntegrityWarning) {
        val isBlocking = integrityMessage.contains("tidak dapat dilanjutkan")
        ErrorDialog(
            title = if (isBlocking) "Peringatan Keamanan" else "Perhatian",
            message = integrityMessage,
            onDismiss = { showIntegrityWarning = false },
            onRetry = if (!isBlocking) {
                { viewModel.submitVote(electionId, candidateId); showIntegrityWarning = false }
            } else null
        )
    }

    // ---- Error ----
    state.error?.let { error ->
        ErrorDialog(
            message = error,
            onDismiss = { viewModel.resetVoteState() }
        )
    }
}

@Composable
private fun BiometricStatusIndicator(status: BiometricResult) {
    val (icon, label, color) = when (status) {
        is BiometricResult.Available -> Triple(Icons.Filled.Fingerprint, "Biometrik Tersedia", EmeraldGreen)
        is BiometricResult.NotEnrolled -> Triple(Icons.Filled.Fingerprint, "Biometrik Tidak Terdaftar", StatusWarning)
        else -> Triple(Icons.Filled.Security, "Verifikasi Alternatif", StatusWarning)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
