package com.example.elsuarku.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Visibility, null, tint = StatusError, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Monitor Sistem", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    Surface(
                        color = if (state.securityAlerts.isEmpty()) EmeraldGreen.copy(alpha = 0.2f) else StatusError.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (state.securityAlerts.isEmpty()) EmeraldGreen else StatusError))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (state.securityAlerts.isEmpty()) "SISTEM AMAN" else "${state.securityAlerts.size} ALERT",
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                color = if (state.securityAlerts.isEmpty()) EmeraldGreen else StatusError
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Pengaturan", tint = OnDeepBlue.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, "Keluar", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlueDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Counters
            item {
                Text("LIVE MONITORING", style = MaterialTheme.typography.labelLarge, color = StatusError, fontWeight = FontWeight.Bold)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveCounter("Total Suara", "${state.totalVotes}", Icons.Filled.HowToVote, EmeraldGreen, Modifier.weight(1f))
                    LiveCounter("Pemilihan", "${state.elections.size}", Icons.Filled.Ballot, DeepBlueLight, Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiveCounter("Security Alerts", "${state.securityAlerts.size}", Icons.Filled.Warning,
                        if (state.securityAlerts.isNotEmpty()) StatusError else EmeraldGreen, Modifier.weight(1f))
                    LiveCounter("Log Audit", "${state.auditLogs.size}", Icons.Filled.Shield, Gold, Modifier.weight(1f))
                }
            }

            // Quick Actions
            item { Text("Menu Monitoring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorMenuCard("Live Stats", "Statistik\nReal-time", Icons.Filled.TrendingUp, EmeraldGreen, Modifier.weight(1f), onClick = { if (state.elections.isNotEmpty()) onNavigateToLiveStats(state.elections.first().id) })
                    MonitorMenuCard("Audit Logs", "Catatan\nAktivitas", Icons.Filled.Shield, DeepBlueLight, Modifier.weight(1f), onClick = onNavigateToAuditLogs)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonitorMenuCard("Security", "Ancaman &\nKeamanan", Icons.Filled.Security, StatusError, Modifier.weight(1f), onClick = onNavigateToSecurity)
                    MonitorMenuCard("Report", "Laporan\nMonitoring", Icons.Filled.Description, Gold, Modifier.weight(1f), onClick = onNavigateToReport)
                }
            }

            // System Health
            item { Text("Kesehatan Sistem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        HealthRow("Firebase Auth", true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        HealthRow("Cloud Firestore", true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        HealthRow("Enkripsi AES-256", true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        HealthRow("App Check", false)
                    }
                }
            }

            // Active Elections
            item {
                Text("Pemilihan Aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark)
            }
            if (state.elections.isEmpty()) {
                item {
                    Text("Menunggu admin membuat pemilihan...", style = MaterialTheme.typography.bodyMedium, color = DeepBlueLight)
                }
            } else {
                items(state.elections) { election ->
                    Card(
                        modifier = Modifier.fillMaxWidth(), onClick = { onNavigateToLiveStats(election.id) },
                        colors = CardDefaults.cardColors(containerColor = DeepBlueSurface), shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TrendingUp, null, tint = DeepBlue, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(election.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${election.votedCount}/${election.totalVoters} suara — ${election.status.name}",
                                    style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = DeepBlueLight)
                        }
                    }
                }
            }

            // Recent Audit Log
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Log Audit Terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark)
                    TextButton(onClick = onNavigateToAuditLogs) { Text("Lihat Semua", color = DeepBlue) }
                }
            }
            if (state.auditLogs.isEmpty()) {
                item { Text("Belum ada aktivitas tercatat", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) }
            } else {
                items(state.auditLogs.take(5)) { log ->
                    val sevColor = when (log.severity) {
                        AuditSeverity.CRITICAL -> StatusError; AuditSeverity.WARNING -> StatusWarning; else -> EmeraldGreen
                    }
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(sevColor))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(log.action.name.replace("_", " "), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                                Text("${log.actorName} — ${log.timestamp.toFormattedDateTime()}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                            Surface(color = sevColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text(log.severity.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = sevColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCounter(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DeepBlueDark)
        }
    }
}

@Composable
private fun MonitorMenuCard(label: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(6.dp)); Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color); Spacer(Modifier.height(2.dp)); Text(desc, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f)) }
    }
}

@Composable
private fun HealthRow(service: String, isOnline: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(service, style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isOnline) EmeraldGreen else StatusError))
            Spacer(Modifier.width(6.dp))
            Text(if (isOnline) "ONLINE" else "OFFLINE", style = MaterialTheme.typography.labelSmall,
                color = if (isOnline) EmeraldGreen else StatusError, fontWeight = FontWeight.Bold)
        }
    }
}
