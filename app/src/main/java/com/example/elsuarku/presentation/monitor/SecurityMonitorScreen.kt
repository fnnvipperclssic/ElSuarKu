package com.example.elsuarku.presentation.monitor

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.PulseDot
import com.example.elsuarku.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityMonitorScreen(viewModel: MonitorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Auto-refresh every 30s while screen is visible
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            viewModel.refresh()
        }
    }

    val hasCritical = state.securityAlerts.any { it.severity == AuditSeverity.CRITICAL }
    val timeFormatter = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = StatusError, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Security Monitor", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnDeepBlue)
                    }
                },
                actions = {
                    if (hasCritical) {
                        PulseDot(color = StatusError, size = 8)
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, null, tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && state.securityAlerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memantau keamanan...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status header
            item {
                val isSafe = state.securityAlerts.isEmpty()
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSafe) EmeraldGreen.copy(alpha = 0.12f)
                                    else StatusError.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSafe) {
                                Icon(Icons.Filled.Shield, null, tint = EmeraldGreen, modifier = Modifier.size(26.dp))
                            } else {
                                PulseDot(color = StatusError, size = 10)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                if (isSafe) "Sistem Aman" else "Terdeteksi Ancaman",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSafe) EmeraldGreen else StatusError
                            )
                            Text(
                                if (isSafe) "Tidak ada ancaman keamanan terdeteksi"
                                else "Ditemukan ${state.securityAlerts.size} peringatan keamanan",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepBlueLight
                            )
                        }
                    }
                }
            }

            // Error banner
            state.error?.let { error ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Gagal memuat data", style = MaterialTheme.typography.labelMedium, color = StatusError, fontWeight = FontWeight.Bold)
                                Text(error, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                        }
                    }
                }
            }

            // Severity summary cards
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecCard("Critical", state.securityAlerts.count { it.severity == AuditSeverity.CRITICAL }, StatusError, Icons.Filled.Warning, Modifier.weight(1f))
                    SecCard("Warning", state.securityAlerts.count { it.severity == AuditSeverity.WARNING }, StatusWarning, Icons.Filled.Security, Modifier.weight(1f))
                    SecCard("Info", state.securityAlerts.count { it.severity == AuditSeverity.INFO }, EmeraldGreen, Icons.Filled.Info, Modifier.weight(1f))
                }
            }

            // Alerts list header
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Security Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark)
                    if (state.securityAlerts.isNotEmpty()) {
                        Surface(
                            color = StatusError.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "${state.securityAlerts.size} alert",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusError
                            )
                        }
                    }
                }
            }

            if (state.securityAlerts.isEmpty() && !state.isLoading && state.error == null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(EmeraldGreen))
                            Spacer(Modifier.width(8.dp))
                            Text("Tidak ada ancaman terdeteksi — Sistem AMAN", style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else if (state.securityAlerts.isNotEmpty()) {
                items(state.securityAlerts) { alert ->
                    val sevColor = when (alert.severity) {
                        AuditSeverity.CRITICAL -> StatusError
                        AuditSeverity.WARNING -> StatusWarning
                        else -> EmeraldGreen
                    }
                    val sevIcon = when (alert.severity) {
                        AuditSeverity.CRITICAL -> Icons.Filled.Warning
                        AuditSeverity.WARNING -> Icons.Filled.Security
                        else -> Icons.Filled.Info
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = sevColor.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(sevIcon, null, tint = sevColor, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(alert.action.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = DeepBlueDark)
                                Spacer(Modifier.height(2.dp))
                                Text("${alert.actorName} | ${alert.targetName}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                Spacer(Modifier.height(1.dp))
                                Text(timeFormatter.format(Date(alert.timestamp)), style = MaterialTheme.typography.labelSmall, color = DeepBlueLight.copy(alpha = 0.5f))
                            }
                            Surface(
                                color = sevColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    alert.severity.name,
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

@Composable
private fun SecCard(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}
