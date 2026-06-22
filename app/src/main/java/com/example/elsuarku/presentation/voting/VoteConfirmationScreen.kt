package com.example.elsuarku.presentation.voting

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.*
import com.example.elsuarku.security.AntiTampering
import com.example.elsuarku.security.BiometricPromptManager
import com.example.elsuarku.security.BiometricResult
import com.example.elsuarku.ui.theme.*

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
    var isBlockingError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember { context as? androidx.fragment.app.FragmentActivity }

    val biometricManager = remember { BiometricPromptManager() }
    val antiTampering = remember { AntiTampering(context) }
    val biometricStatus = remember {
        activity?.let { biometricManager.canAuthenticate(it) } ?: BiometricResult.Unknown
    }

    LaunchedEffect(electionId, candidateId) {
        viewModel.loadCandidateForVote(electionId, candidateId)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            kotlinx.coroutines.delay(400)
            onVoteSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfirmasi Pilihan", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SecurityBadge(level = SecurityLevel.VERIFIED)

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Anda akan memilih:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            // Candidate card
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bitmap = remember(state.candidate?.photoBase64) {
                        state.candidate?.photoBase64?.let {
                            try {
                                val bytes = Base64.decode(it, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Exception) { null }
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = state.candidate?.name,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(3.dp, Gold.copy(alpha = 0.4f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(DeepBlueLight, DeepBlue)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, null, tint = OnDeepBlue, modifier = Modifier.size(44.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = state.candidate?.name ?: "",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlue,
                        textAlign = TextAlign.Center
                    )

                    if ((state.candidate?.nomorUrut ?: 0) > 0) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = Gold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Nomor Urut: ${state.candidate?.nomorUrut}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Gold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Biometric status
            BiometricStatusIndicator(biometricStatus)

            Spacer(Modifier.height(16.dp))

            // Warning
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StatusWarning.copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.Warning, null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Pilihan bersifat permanen dan tidak dapat diubah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusWarning
                )
            }

            Spacer(Modifier.height(28.dp))

            // Confirm button
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isSubmitting) {
                    LoadingIndicator()
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Konfirmasi Pilihan Saya",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // ── Confirmation Dialog → Integrity Check → Biometric → Submit ──
    if (showConfirmDialog) {
        ConfirmationDialog(
            title = "Verifikasi Identitas",
            message = "Apakah Anda yakin ingin memberikan suara kepada ${state.candidate?.name}? " +
                    "Tindakan ini memerlukan verifikasi keamanan dan tidak dapat dibatalkan.",
            confirmText = "Ya, Pilih Sekarang",
            onConfirm = {
                showConfirmDialog = false
                val integrity = antiTampering.performFullCheck()
                if (!integrity.isSafe) {
                    isBlockingError = true
                    integrityMessage = "⚠️ Masalah keamanan terdeteksi: ${integrity.threats.joinToString(", ")}.\n\nVoting tidak dapat dilanjutkan demi keamanan suara Anda."
                    showIntegrityWarning = true
                    return@ConfirmationDialog
                }
                if (activity == null) {
                    viewModel.submitVote(electionId, candidateId)
                    return@ConfirmationDialog
                }
                if (biometricStatus.isAvailable || biometricStatus.isNotEnrolled) {
                    biometricManager.authenticate(
                        activity = activity,
                        title = "Verifikasi Biometrik",
                        subtitle = "Konfirmasikan identitas Anda untuk memberikan suara",
                        allowDeviceCredential = true,
                        onSuccess = { viewModel.submitVote(electionId, candidateId) },
                        onError = { error ->
                            isBlockingError = false
                            integrityMessage = "$error\n\nAnda dapat mencoba lagi atau melanjutkan tanpa verifikasi biometrik."
                            showIntegrityWarning = true
                        },
                        onCancel = { /* stay on screen */ }
                    )
                } else {
                    isBlockingError = false
                    integrityMessage = "Biometrik tidak tersedia: ${biometricStatus.userFriendlyMessage}\n\nAnda tetap dapat melanjutkan voting tanpa verifikasi biometrik."
                    showIntegrityWarning = true
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    if (showIntegrityWarning) {
        ErrorDialog(
            title = if (isBlockingError) "⚠️ Peringatan Keamanan" else "Verifikasi Biometrik",
            message = integrityMessage,
            onDismiss = { showIntegrityWarning = false },
            onRetry = if (!isBlockingError) {
                {
                    showIntegrityWarning = false
                    if (activity != null) {
                        biometricManager.authenticate(
                            activity = activity,
                            title = "Verifikasi Biometrik",
                            subtitle = "Konfirmasikan identitas Anda untuk memberikan suara",
                            allowDeviceCredential = true,
                            onSuccess = { viewModel.submitVote(electionId, candidateId) },
                            onError = { error ->
                                isBlockingError = false
                                integrityMessage = "$error\n\nAnda dapat mencoba lagi atau melanjutkan tanpa verifikasi biometrik."
                                showIntegrityWarning = true
                            },
                            onCancel = { }
                        )
                    } else {
                        viewModel.submitVote(electionId, candidateId)
                    }
                }
            } else null,
            secondaryButton = if (!isBlockingError) {
                Pair({
                    showIntegrityWarning = false
                    viewModel.submitVote(electionId, candidateId)
                }, "Lanjutkan Tanpa Biometrik")
            } else null
        )
    }

    state.error?.let { error ->
        ErrorDialog(message = error, onDismiss = { viewModel.clearVoteError() })
    }
}

@Composable
private fun BiometricStatusIndicator(status: BiometricResult) {
    val (icon, label, color) = when (status) {
        is BiometricResult.Available -> Triple(Icons.Filled.Fingerprint, "Biometrik Tersedia", EmeraldGreen)
        is BiometricResult.NotEnrolled -> Triple(Icons.Filled.Fingerprint, "Biometrik Tidak Terdaftar", StatusWarning)
        else -> Triple(Icons.Filled.Security, "Verifikasi Alternatif", StatusWarning)
    }

    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = color)
        }
    }
}
