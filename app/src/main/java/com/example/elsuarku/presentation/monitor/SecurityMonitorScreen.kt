package com.example.elsuarku.presentation.monitor

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
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityMonitorScreen(viewModel: MonitorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Security, null, tint = StatusError, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Security Monitor", color = OnDeepBlue, fontWeight = FontWeight.Bold) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnDeepBlue) } }, actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Filled.Refresh, null, tint = OnDeepBlue) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue))
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.05f)), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = StatusError, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column { Text("Sistem Monitoring Aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusError); Text("Mendeteksi ancaman keamanan secara real-time", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecCard("Critical", state.securityAlerts.count { it.severity == AuditSeverity.CRITICAL }, StatusError, Modifier.weight(1f))
                    SecCard("Warning", state.securityAlerts.count { it.severity == AuditSeverity.WARNING }, StatusWarning, Modifier.weight(1f))
                    SecCard("Info", state.securityAlerts.count { it.severity == AuditSeverity.INFO }, EmeraldGreen, Modifier.weight(1f))
                }
            }

            item { Text("Security Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
            if (state.securityAlerts.isEmpty()) {
                item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = EmeraldGreenSurface)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(EmeraldGreen)); Spacer(Modifier.width(8.dp)); Text("Tidak ada ancaman terdeteksi — Sistem AMAN", style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Medium) } } }
            } else {
                items(state.securityAlerts) { alert ->
                    val sevColor = when (alert.severity) { AuditSeverity.CRITICAL -> StatusError; AuditSeverity.WARNING -> StatusWarning; else -> EmeraldGreen }
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = sevColor.copy(alpha = 0.05f)), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Filled.Warning, null, tint = sevColor, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(alert.action.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = sevColor); Text("${alert.actorName} | ${alert.targetName}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecCard(label: String, value: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color); Text(label, style = MaterialTheme.typography.labelSmall, color = color) }
    }
}
