package com.example.elsuarku.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.ui.theme.*
import com.example.elsuarku.utils.Resource
import com.example.elsuarku.utils.toFormattedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(viewModel: AdminViewModel, auditRepository: AuditRepository, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var allLogs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.loadAllData()
        when (val r = auditRepository.getLogsBySeverity(AuditSeverity.WARNING, 50)) {
            is Resource.Success -> allLogs = r.data
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Security Center", color = OnDeepBlue, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnDeepBlue) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue))
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Pusat Keamanan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DeepBlueDark) }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecurityStat("Critical Alerts", state.securityAlerts.size, StatusError, Modifier.weight(1f))
                    SecurityStat("Warnings", allLogs.size, StatusWarning, Modifier.weight(1f))
                }
            }

            item { Text("Activity Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
            items(allLogs.take(20)) { log ->
                val sevColor = when (log.severity) { AuditSeverity.CRITICAL -> StatusError; AuditSeverity.WARNING -> StatusWarning; else -> EmeraldGreen }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = sevColor.copy(alpha = 0.05f)), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Security, null, tint = sevColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(log.action.name.replace("_", " "), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = DeepBlueDark); Text("${log.actorName} | ${log.timestamp.toFormattedDateTime()}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityStat(label: String, value: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("$value", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = color); Text(label, style = MaterialTheme.typography.labelSmall, color = color) }
    }
}
