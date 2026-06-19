package com.example.elsuarku.presentation.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue
import com.example.elsuarku.ui.theme.SoftWhite
import com.example.elsuarku.ui.theme.StatusError
import com.example.elsuarku.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                title = { Text("Log Audit", color = OnDeepBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingIndicator(message = "Memuat log audit…")
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
                            Icon(Icons.Filled.Warning, null, tint = StatusError, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Peringatan Keamanan (${state.securityAlerts.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = StatusError)
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    items(state.securityAlerts) { log ->
                        DetailedAuditLogItem(log)
                    }

                    item {
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Semua Aktivitas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DeepBlueDark
                        )
                    }
                }

                if (state.auditLogs.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada log aktivitas.",
                            color = DeepBlueLight,
                            modifier = Modifier.padding(16.dp)
                        )
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
            Icon(
                imageVector = severityIcon,
                contentDescription = null,
                tint = severityColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.action.name.replace("_", " "),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = DeepBlueDark
                    )
                    Text(
                        text = log.severity.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
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
                    text = formatFullTime(log.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBlueDark.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private fun formatFullTime(timestamp: Long): String {
    val fmt = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id"))
    return fmt.format(Date(timestamp))
}
