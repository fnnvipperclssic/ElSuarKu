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
import androidx.fragment.app.FragmentActivity
import com.example.elsuarku.utils.unwrapFragmentActivity

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
    var showBiometricDialog by remember { mutableStateOf(false) }
    var biometricMessage by remember { mutableStateOf("") }
    var isBlockingError by remember { mutableStateOf(false) }
    var biometricAttempts by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val biometricManager = remember { BiometricPromptManager() }
    val antiTampering = remember { AntiTampering(context) }
    var biometricStatus by remember { mutableStateOf<BiometricResult?>(null) }

    // Resolve FragmentActivity dynamically — NOT via remember {}
    // because LocalContext.current is a ContextThemeWrapper, and the first
    // composition pass may complete before the wrapper chain is fully stable.
    var activity by remember { mutableStateOf<FragmentActivity?>(null) }
    LaunchedEffect(Unit) {
        activity = context.unwrapFragmentActivity()
    }

    // Dynamic biometric availability check — recomputes when activity is ready
    LaunchedEffect(activity) {
        biometricStatus = activity?.let { biometricManager.canAuthenticate(it) } ?: BiometricResult.Unknown
    }

    // Lambda for launching biometric prompt — defined early so it can be
    // referenced from dialog callbacks below.
    // Uses context.unwrapFragmentActivity() on each call to handle wrapped contexts.
    val launchBiometricPrompt: () -> Unit = {
        val act = context.unwrapFragmentActivity()
        if (act == null) {
            isBlockingError = true
            biometricMessage = "Terjadi kesalahan sistem. Tidak dapat membuka verifikasi biometrik."
            showBiometricDialog = true
        } else {
            biometricAttempts++
            biometricManager.authenticate(
                activity = act,
                title = "Verifikasi Biometrik",
                subtitle = "Konfirmasikan identitas Anda untuk memberikan suara",
                allowDeviceCredential = true, // PIN/pola accepted as fallback
                onSuccess = {
                    biometricAttempts = 0
                    viewModel.setBiometricVerified()
                    viewModel.submitVote(electionId, candidateId)
                },
                onError = { error ->
                    isBlockingError = false
                    biometricMessage = "$error\n\nVerifikasi identitas WAJIB untuk memberikan suara. Silakan coba lagi."
                    showBiometricDialog = true
                },
                onCancel = {
                    isBlockingError = false
                    biometricMessage = "Verifikasi dibatalkan. Anda WAJIB menyelesaikan verifikasi identitas untuk memberikan suara."
                    showBiometricDialog = true
                }
            )
        }
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
                                val bytes = Base64.decode(it, Base64.NO_WRAP)
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
            BiometricStatusIndicator(biometricStatus ?: BiometricResult.Unknown)

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

    // ── Confirmation Dialog → Integrity Check → Biometric (Mandatory) → Submit ──
    if (showConfirmDialog) {
        ConfirmationDialog(
            title = "Verifikasi Identitas",
            message = "Apakah Anda yakin ingin memberikan suara kepada ${state.candidate?.name}? " +
                    "Verifikasi biometrik diperlukan untuk melanjutkan. Tindakan ini tidak dapat dibatalkan.",
            confirmText = "Ya, Pilih Sekarang",
            onConfirm = {
                showConfirmDialog = false
                // Step 1: Integrity check — blocking if threats detected
                val integrity = antiTampering.performFullCheck()
                if (!integrity.isSafe) {
                    isBlockingError = true
                    biometricMessage = "⚠️ Masalah keamanan terdeteksi: ${integrity.threats.joinToString(", ")}.\n\nVoting tidak dapat dilanjutkan demi keamanan suara Anda."
                    showBiometricDialog = true
                    return@ConfirmationDialog
                }
                // Step 2: Check biometric availability
                if (activity == null) {
                    isBlockingError = true
                    biometricMessage = "Terjadi kesalahan sistem. Tidak dapat membuka verifikasi biometrik.\nSilakan coba lagi."
                    showBiometricDialog = true
                    return@ConfirmationDialog
                }
                val bioStatus = biometricStatus
                if (bioStatus == null || (bioStatus !is BiometricResult.Available && bioStatus !is BiometricResult.NotEnrolled)) {
                    isBlockingError = true
                    biometricMessage = if (bioStatus == null) "Sedang memeriksa biometrik. Silakan coba lagi dalam beberapa saat."
                    else "Biometrik tidak tersedia: ${bioStatus.userFriendlyMessage}\n\n" +
                            "Verifikasi identitas WAJIB untuk memberikan suara. " +
                            "Pastikan perangkat Anda memiliki sensor biometrik yang berfungsi."
                    showBiometricDialog = true
                    return@ConfirmationDialog
                }
                // Step 3: Launch biometric prompt (mandatory — no skip)
                biometricAttempts = 0
                launchBiometricPrompt()
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    // ── Biometric Result Dialog ──
    if (showBiometricDialog) {
        if (isBlockingError) {
            // Integrity failure or biometric hardware unavailable — BLOCK voting
            ErrorDialog(
                title = "⚠️ Verifikasi Gagal",
                message = biometricMessage,
                onDismiss = { showBiometricDialog = false },
                onRetry = null,
                secondaryButton = null
            )
        } else {
            // Biometric failed but can retry
            ErrorDialog(
                title = "Verifikasi Biometrik",
                message = biometricMessage,
                onDismiss = { showBiometricDialog = false },
                onRetry = { showBiometricDialog = false; launchBiometricPrompt() },
                secondaryButton = null // No skip — biometric is mandatory
            )
        }
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
        is BiometricResult.Unknown -> Triple(Icons.Filled.Security, "Memeriksa Biometrik...", StatusWarning)
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
