package com.example.elsuarku.presentation.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.PulseDot
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue
import com.example.elsuarku.ui.theme.SoftWhite
import com.example.elsuarku.ui.theme.StatusError
import com.example.elsuarku.ui.theme.StatusWarning
import com.example.elsuarku.utils.toFormattedDateTimeFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    viewModel: MonitorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = OnDeepBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log Audit", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && state.auditLogs.isEmpty() && state.securityAlerts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingIndicator(message = "Memuat log audit…")
            }
        } else if (state.error != null && state.auditLogs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Gagal memuat log audit", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) { Text("Coba Lagi") }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftWhite)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Security alerts section
                if (state.securityAlerts.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulseDot(color = StatusError, size = 10)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Warning, null, tint = StatusError, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Peringatan Keamanan (${state.securityAlerts.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusError
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(state.securityAlerts) { log ->
                        DetailedAuditLogItem(log)
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(4.dp),
                                shape = RoundedCornerShape(2.dp),
                                color = DeepBlue
                            ) {}
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Semua Aktivitas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DeepBlueDark
                            )
                        }
                    }
                }

                if (state.auditLogs.isEmpty() && state.securityAlerts.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Shield, null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Belum ada aktivitas tercatat", color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                                Text("Log akan muncul setelah aktivitas dilakukan", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                        }
                    }
                } else {
                    items(state.auditLogs) { log ->
                        DetailedAuditLogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedAuditLogItem(log: AuditLog) {
    val severityColor = when (log.severity) {
        AuditSeverity.CRITICAL -> StatusError
        AuditSeverity.WARNING -> StatusWarning
        AuditSeverity.INFO -> EmeraldGreen
    }

    val severityIcon = when (log.severity) {
        AuditSeverity.CRITICAL -> Icons.Filled.Warning
        AuditSeverity.WARNING -> Icons.Filled.Security
        AuditSeverity.INFO -> Icons.Filled.Shield
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = severityColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = severityIcon,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.action.name.replace("_", " "),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepBlueDark
                    )
                    Surface(
                        color = severityColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = log.severity.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = severityColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Oleh: ${log.actorName} (${log.actorRole})",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBlueDark.copy(alpha = 0.6f)
                )
                if (log.targetName.isNotBlank()) {
                    Text(
                        text = "Target: ${log.targetName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueDark.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = log.timestamp.toFormattedDateTimeFull(),
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBlueDark.copy(alpha = 0.4f)
                )
            }
        }
    }
}

