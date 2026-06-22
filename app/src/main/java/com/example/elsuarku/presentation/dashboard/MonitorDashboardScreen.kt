package com.example.elsuarku.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.PulseDot
import com.example.elsuarku.presentation.monitor.MonitorViewModel
import com.example.elsuarku.ui.theme.*
import com.example.elsuarku.utils.toFormattedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDashboardScreen(
    viewModel: MonitorViewModel,
    onNavigateToLiveStats: (String) -> Unit,
    onNavigateToAuditLogs: () -> Unit,
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val isSystemSafe = state.securityAlerts.isEmpty()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Visibility, null, tint = StatusError, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Monitor Sistem", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    Surface(
                        color = if (isSystemSafe) EmeraldGreen.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isSystemSafe) {
                                PulseDot(color = StatusError, size = 6)
                                Spacer(Modifier.width(4.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGreen)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                if (isSystemSafe) "SISTEM AMAN" else "${state.securityAlerts.size} ALERT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSystemSafe) EmeraldGreen else StatusError
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Pengaturan", tint = OnDeepBlue.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Keluar", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && state.elections.isEmpty() && state.totalVotes == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memuat data monitoring...")
            }
            return@Scaffold
        }

        if (state.error != null && state.elections.isEmpty() && state.totalVotes == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Gagal memuat data monitoring", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) { Text("Coba Lagi") }
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftWhite)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── MONITOR IDENTITY BADGE ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(listOf(DeepBlue, DeepBlueDark))
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulseDot(color = if (isSystemSafe) EmeraldGreen else StatusError, size = 8)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Visibility, null, tint = Gold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "MONITOR",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Gold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (state.monitorName.isNotBlank()) "Login sebagai: ${state.monitorName}"
                            else "Mode Monitoring Aktif",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnDeepBlue.copy(alpha = 0.9f)
                        )
                        Text(
                            "Pantau pemilihan secara real-time. Akses read-only — tanpa hak edit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnDeepBlue.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            // ── LIVE COUNTERS ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(color = StatusError, size = 8)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "LIVE MONITORING",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = StatusError
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveCounter("Total Suara", "${state.totalVotes}", Icons.Filled.HowToVote, EmeraldGreen, Modifier.weight(1f))
                    LiveCounter("Pemilihan", "${state.elections.size}", Icons.Filled.Ballot, DeepBlueLight, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveCounter("User Terdaftar", "${state.totalUsers}", Icons.Filled.Group, Gold, Modifier.weight(1f))
                    LiveCounter("Security Alerts", "${state.securityAlerts.size}", Icons.Filled.Warning,
                        if (state.securityAlerts.isNotEmpty()) StatusError else EmeraldGreen, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveCounter("Log Audit", "${state.auditLogs.size}", Icons.Filled.Shield, DeepBlue, Modifier.weight(1f))
                    val participationRate = if (state.totalUsers > 0) state.totalVotes.toFloat() / state.totalUsers * 100f else 0f
                    LiveCounter("Partisipasi", "${"%.1f".format(participationRate)}%", Icons.AutoMirrored.Filled.TrendingUp, EmeraldGreen, Modifier.weight(1f))
                }
            }

            // ── MENU MONITORING ──
            item {
                Text(
                    "Menu Monitoring",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorMenuCard("Live Stats", "Statistik\nReal-time", Icons.AutoMirrored.Filled.TrendingUp, EmeraldGreen, Modifier.weight(1f), onClick = {
                        if (state.elections.isNotEmpty()) onNavigateToLiveStats(state.elections.first().id)
                        else Toast.makeText(context, "Belum ada pemilihan aktif untuk dimonitor", Toast.LENGTH_SHORT).show()
                    })
                    MonitorMenuCard("Audit Logs", "Catatan\nAktivitas", Icons.Filled.Shield, DeepBlueLight, Modifier.weight(1f), onClick = onNavigateToAuditLogs)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorMenuCard("Security", "Ancaman &\nKeamanan", Icons.Filled.Security, StatusError, Modifier.weight(1f), onClick = onNavigateToSecurity)
                    MonitorMenuCard("Report", "Laporan\nMonitoring", Icons.Filled.Description, Gold, Modifier.weight(1f), onClick = onNavigateToReport)
                }
            }

            // ── SYSTEM HEALTH ──
            item {
                Text(
                    "Kesehatan Sistem",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
            item {
                val isAuthOk = state.monitorName.isNotBlank()
                val isFirestoreOk = state.error == null
                val isSessionOk = state.monitorName.isNotBlank()

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    HealthRow("Firebase Auth", isAuthOk, if (isAuthOk) "TERHUBUNG" else "MENUNGGU")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = DeepBlueLight.copy(alpha = 0.1f)
                    )
                    HealthRow("Cloud Firestore", isFirestoreOk, if (isFirestoreOk) "AKTIF" else "ERROR")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = DeepBlueLight.copy(alpha = 0.1f)
                    )
                    HealthRow("Enkripsi AES-256", true, "AKTIF")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = DeepBlueLight.copy(alpha = 0.1f)
                    )
                    HealthRow("Session Monitor", isSessionOk, if (isSessionOk) "AKTIF" else "BELUM LOGIN")
                }
            }

            // ── ACTIVE ELECTIONS ──
            item {
                Text(
                    "Pemilihan Aktif",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
            if (state.elections.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Menunggu pemilihan dibuat", color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                            Text("Admin akan membuat pemilihan melalui panel administrasi", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        }
                    }
                }
            } else {
                items(state.elections) { election ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        onClick = { onNavigateToLiveStats(election.id) }
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = DeepBlue, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(election.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = DeepBlueDark)
                                Text(
                                    "${election.votedCount}/${election.totalVoters} suara — ${election.status.name}",
                                    style = MaterialTheme.typography.bodySmall, color = DeepBlueLight
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = DeepBlueLight)
                        }
                    }
                }
            }

            // ── RECENT AUDIT LOG ──
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Log Audit Terbaru",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlueDark
                    )
                    TextButton(onClick = onNavigateToAuditLogs) {
                        Text("Lihat Semua", color = DeepBlue)
                    }
                }
            }
            if (state.auditLogs.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.History, null, tint = DeepBlueLight, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Belum ada aktivitas tercatat", style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                            Text("Log akan muncul setelah ada aktivitas di sistem", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        }
                    }
                }
            } else {
                items(state.auditLogs.take(5)) { log ->
                    val sevColor = when (log.severity) {
                        AuditSeverity.CRITICAL -> StatusError
                        AuditSeverity.WARNING -> StatusWarning
                        else -> EmeraldGreen
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(sevColor))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    log.action.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = DeepBlueDark
                                )
                                Text(
                                    "${log.actorName} — ${log.timestamp.toFormattedDateTime()}",
                                    style = MaterialTheme.typography.bodySmall, color = DeepBlueLight
                                )
                            }
                            Surface(
                                color = sevColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    log.severity.name,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = sevColor
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun LiveCounter(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = DeepBlueDark
        )
    }
}

@Composable
private fun MonitorMenuCard(
    label: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier, cornerRadius = 16.dp, onClick = onClick) {
        Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
        Spacer(Modifier.height(4.dp))
        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun HealthRow(service: String, isHealthy: Boolean, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(service, style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isHealthy) EmeraldGreen else StatusError)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isHealthy) EmeraldGreen else StatusError
            )
        }
    }
}
