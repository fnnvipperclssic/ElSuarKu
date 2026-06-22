package com.example.elsuarku.presentation.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorReportScreen(viewModel: MonitorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var selectedElectionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Load election stats when a new election is selected
    LaunchedEffect(selectedElectionId) {
        selectedElectionId?.let { viewModel.loadElectionStats(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Assessment, null, tint = OnDeepBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Laporan Monitoring", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && state.elections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memuat data laporan...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Laporan Monitoring", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DeepBlueDark)
            }
            item {
                Text("Pilih pemilihan untuk melihat detail:", style = MaterialTheme.typography.bodyMedium, color = DeepBlueLight)
            }

            // Summary stats
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportCard("Total Suara", "${state.totalVotes}", EmeraldGreen, Icons.Filled.HowToVote, Modifier.weight(1f))
                    ReportCard("Pemilihan", "${state.elections.size}", DeepBlue, Icons.Filled.Ballot, Modifier.weight(1f))
                    ReportCard("Alerts", "${state.securityAlerts.size}", StatusError, Icons.Filled.Warning, Modifier.weight(1f))
                }
            }

            // Error display
            state.error?.let { error ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, style = MaterialTheme.typography.bodySmall, color = StatusError)
                        }
                    }
                }
            }

            if (state.elections.isEmpty() && !state.isLoading && state.error == null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Belum ada data", color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                            Text("Pemilihan akan muncul setelah dibuat oleh Admin", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        }
                    }
                }
            } else if (state.elections.isNotEmpty()) {
                items(state.elections) { election ->
                    val isSelected = selectedElectionId == election.id
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        onClick = { selectedElectionId = election.id }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EmeraldGreen.copy(alpha = 0.12f) else DeepBlue.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Assessment, null,
                                        tint = if (isSelected) EmeraldGreen else DeepBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(election.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = DeepBlue.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "${election.votedCount}/${election.totalVoters} suara",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepBlueLight
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(election.status.name, style = MaterialTheme.typography.labelSmall, color = DeepBlueLight)
                                }
                            }
                            if (isSelected) {
                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldGreen.copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedElectionId != null && state.selectedElection != null) {
                    item {
                        val sel = state.selectedElection!!
                        val candidateVoteCount = state.candidates.sumOf { it.voteCount }
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = EmeraldGreen.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Assessment, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text("Ringkasan: ${sel.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = DeepBlue.copy(alpha = 0.06f))
                                Spacer(Modifier.height(12.dp))
                                SummaryRow("Partisipasi", "${state.electionVoteCount}/${sel.totalVoters} (${if (sel.totalVoters > 0) (state.electionVoteCount.toFloat() / sel.totalVoters * 100).toInt() else 0}%)")
                                SummaryRow("Status", sel.status.name)
                                SummaryRow("Kandidat", "${state.candidates.size}")
                                SummaryRow("Total Suara", "$candidateVoteCount")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = DeepBlueLight)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = DeepBlueDark)
    }
}

@Composable
private fun ReportCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}
