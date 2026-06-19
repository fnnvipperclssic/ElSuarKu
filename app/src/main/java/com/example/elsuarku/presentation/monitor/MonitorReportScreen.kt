package com.example.elsuarku.presentation.monitor

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
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorReportScreen(viewModel: MonitorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var selectedElection by remember { mutableStateOf<Election?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Laporan Monitoring", color = OnDeepBlue, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnDeepBlue) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue))
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Export Laporan Monitoring", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
            item { Text("Pilih pemilihan untuk generate laporan monitoring:", style = MaterialTheme.typography.bodyMedium, color = DeepBlueLight) }

            // Summary stats
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportCard("Total Suara", "${state.totalVotes}", EmeraldGreen, Modifier.weight(1f))
                    ReportCard("Pemilihan", "${state.elections.size}", DeepBlueLight, Modifier.weight(1f))
                    ReportCard("Alerts", "${state.securityAlerts.size}", StatusError, Modifier.weight(1f))
                }
            }

            if (state.elections.isEmpty()) {
                item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueSurface)) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp)); Text("Belum ada data", color = DeepBlueDark) } } }
            } else {
                items(state.elections) { election ->
                    Card(onClick = { selectedElection = election; viewModel.loadElectionStats(election.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (selectedElection == election) EmeraldGreenSurface else SurfaceWhite), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Assessment, null, tint = DeepBlue, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(election.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DeepBlueDark); Text("${election.votedCount}/${election.totalVoters} suara", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) }
                            if (selectedElection == election) Icon(Icons.Filled.CheckCircle, null, tint = EmeraldGreen)
                        }
                    }
                }

                if (selectedElection != null) {
                    item {
                        val sel = selectedElection!!
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = EmeraldGreenSurface), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Ringkasan: ${sel.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldGreenDark)
                                Spacer(Modifier.height(8.dp))
                                Text("Partisipasi: ${state.electionVoteCount}/${sel.totalVoters} (${if (sel.totalVoters > 0) (state.electionVoteCount.toFloat() / sel.totalVoters * 100).toInt() else 0}%)", style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark)
                                Text("Status: ${sel.status.name}", style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark)
                                Text("Security Alerts: ${state.securityAlerts.size}", style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark)
                                Text("Total Audit Logs: ${state.auditLogs.size}", style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color); Text(label, style = MaterialTheme.typography.labelSmall, color = color) }
    }
}
