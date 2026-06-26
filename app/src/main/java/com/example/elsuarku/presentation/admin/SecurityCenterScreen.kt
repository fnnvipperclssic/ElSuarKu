package com.example.elsuarku.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.PulseDot
import com.example.elsuarku.security.KeyAttestationVerifier
import com.example.elsuarku.security.SslInterceptionDetector
import com.example.elsuarku.ui.theme.*
import com.example.elsuarku.utils.Constants
import com.example.elsuarku.utils.toFormattedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Center", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnDeepBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllData() }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.securityAlerts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    LoadingIndicator(message = "Memuat data keamanan...")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.securityAlerts.any { it.severity == AuditSeverity.CRITICAL }) {
                                PulseDot(color = StatusError, size = 8)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                "Pusat Keamanan",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = DeepBlueDark
                            )
                        }
                    }

                    // Severity summary
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecurityStat(
                                "Critical",
                                state.securityAlerts.count { it.severity == AuditSeverity.CRITICAL },
                                StatusError, Icons.Filled.Warning, Modifier.weight(1f)
                            )
                            SecurityStat(
                                "Warning",
                                state.securityAlerts.count { it.severity == AuditSeverity.WARNING },
                                StatusWarning, Icons.Filled.Security, Modifier.weight(1f)
                            )
                            SecurityStat(
                                "Info",
                                state.securityAlerts.count { it.severity == AuditSeverity.INFO },
                                EmeraldGreen, Icons.Filled.Info, Modifier.weight(1f)
                            )
                        }
                    }

                    // ── Security Diagnostics ──
                    item {
                        // Run security diagnostics off the main thread to avoid jank
                        var attestationReport by remember {
                            mutableStateOf<KeyAttestationVerifier.AttestationReport?>(null)
                        }
                        var sslResult by remember {
                            mutableStateOf<SslInterceptionDetector.Result?>(null)
                        }
                        var diagnosticsRun by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            if (!diagnosticsRun) {
                                diagnosticsRun = true
                                val attest = withContext(Dispatchers.Default) {
                                    runCatching {
                                        KeyAttestationVerifier.verifyKeyAttestation(Constants.KEY_ALIAS)
                                    }.getOrNull()
                                }
                                val ssl = withContext(Dispatchers.Default) {
                                    runCatching {
                                        SslInterceptionDetector.checkForInterception()
                                    }.getOrNull()
                                }
                                attestationReport = attest
                                sslResult = ssl
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Diagnostik Keamanan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepBlueDark
                            )

                            // Key Attestation Card
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val attestSecure = attestationReport?.isSecure == true
                                    Icon(
                                        Icons.Filled.Key,
                                        null,
                                        tint = if (attestSecure) EmeraldGreen else StatusError,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (attestSecure) "Key Attestation: Hardware-Backed"
                                            else "Key Attestation: Not Verified",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = DeepBlueDark
                                        )
                                        val attestReport = attestationReport
                                        if (attestReport != null) {
                                            val bootLabel = when (attestReport.verifiedBootState) {
                                                KeyAttestationVerifier.AttestationReport.BootState.VERIFIED -> "Verified Boot ✓"
                                                KeyAttestationVerifier.AttestationReport.BootState.SELF_SIGNED -> "Self-Signed Boot ⚠"
                                                KeyAttestationVerifier.AttestationReport.BootState.UNVERIFIED -> "Unverified Boot ✗"
                                                KeyAttestationVerifier.AttestationReport.BootState.FAILED -> "Boot State: FAILED ✗"
                                            }
                                            Text(
                                                "$bootLabel · StrongBox: ${if (attestReport.isStrongBox) "Yes" else "No"} · TEE: ${if (attestReport.isTeeEnforced) "Yes" else "No"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DeepBlueLight
                                            )
                                        } else {
                                            Text(
                                                "Tidak dapat memverifikasi — pastikan device mendukung hardware keystore",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DeepBlueLight
                                            )
                                        }
                                    }
                                    Surface(
                                        color = (if (attestSecure) EmeraldGreen else StatusError).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (attestSecure) "AMAN" else "⚠",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (attestSecure) EmeraldGreen else StatusError
                                        )
                                    }
                                }
                            }

                            // SSL Interception Card
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val sslClean = sslResult == SslInterceptionDetector.Result.CLEAN
                                    Icon(
                                        Icons.Filled.Lock,
                                        null,
                                        tint = if (sslClean) EmeraldGreen else StatusWarning,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (sslClean) "SSL/TLS: Aman"
                                            else "SSL/TLS: ${sslResult?.name ?: "Unknown"}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = DeepBlueDark
                                        )
                                        Text(
                                            if (sslClean) "Tidak ada user CA certificate atau proxy terdeteksi"
                                            else "Potensi SSL interception terdeteksi — periksa certificate store device",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DeepBlueLight
                                        )
                                    }
                                    Surface(
                                        color = (if (sslClean) EmeraldGreen else StatusWarning).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (sslClean) "AMAN" else "⚠",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (sslClean) EmeraldGreen else StatusWarning
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Alerts list
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Aktivitas Keamanan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepBlueDark
                            )
                            if (state.securityAlerts.isNotEmpty()) {
                                Text(
                                    "${state.securityAlerts.size} log",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepBlueLight
                                )
                            }
                        }
                    }

                    if (state.error != null) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Gagal Memuat Data", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                                    Text(state.error ?: "Terjadi kesalahan", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(onClick = { viewModel.loadAllData() }) { Text("Coba Lagi") }
                                }
                            }
                        }
                    } else if (state.securityAlerts.isEmpty() && !state.isLoading) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Shield, null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Sistem Aman", style = MaterialTheme.typography.titleMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                    Text("Tidak ada ancaman keamanan terdeteksi", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                }
                            }
                        }
                    } else {
                        items(state.securityAlerts) { log ->
                            val sevColor = when (log.severity) {
                                AuditSeverity.CRITICAL -> StatusError
                                AuditSeverity.WARNING -> StatusWarning
                                else -> EmeraldGreen
                            }
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(sevColor)
                                            .offset(y = 4.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            log.action.name.replace("_", " "),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                            color = DeepBlueDark
                                        )
                                        Text(
                                            "${log.actorName} — ${log.timestamp.toFormattedDateTime()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DeepBlueLight
                                        )
                                        if (log.targetName.isNotBlank()) {
                                            Text(
                                                "Target: ${log.targetName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepBlueLight.copy(alpha = 0.55f)
                                            )
                                        }
                                    }
                                    Surface(
                                        color = sevColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            log.severity.name,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = sevColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityStat(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text("$value", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}
