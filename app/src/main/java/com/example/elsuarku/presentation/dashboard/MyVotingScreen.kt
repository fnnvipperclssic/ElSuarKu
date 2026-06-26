package com.example.elsuarku.presentation.dashboard

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVotingScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Voting Saya", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    LoadingIndicator(message = "Memuat data voting...")
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.padding(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Gagal memuat data", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                            Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.loadDashboard(forceReload = true) }) { Text("Coba Lagi") }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.HowToVote, null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Status Voting", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldGreen)
                                    Text(
                                        "Anda telah memilih di ${state.votedElectionIds.size} pemilihan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EmeraldGreenDark
                                    )
                                    Text(
                                        "(termasuk pemilihan yang sudah selesai)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldGreenDark.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Show only elections that have voting history data (voted + all),
                    // NOT active elections which includes un-voted ones
                    val historyElections = state.allElections
                        .filter { it.id in state.votedElectionIds }
                        .ifEmpty { state.activeElections.filter { it.id in state.votedElectionIds } }
                    if (historyElections.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Info, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Belum ada pemilihan", color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                                    Text("Riwayat voting akan muncul setelah Admin membuat pemilihan", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                }
                            }
                        }
                    } else {
                        items(historyElections, key = { it.id }) { election ->
                            val hasVoted = election.id in state.votedElectionIds
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (hasVoted) EmeraldGreen.copy(alpha = 0.12f)
                                                else Gold.copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (hasVoted) Icons.Filled.CheckCircle else Icons.Filled.Pending,
                                            null,
                                            tint = if (hasVoted) EmeraldGreen else Gold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            election.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = DeepBlueDark
                                        )
                                        Text(
                                            if (hasVoted) "SUDAH MEMILIH" else "Belum memilih",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (hasVoted) EmeraldGreen else Gold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { viewModel.loadDashboard(forceReload = true) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Segarkan Data")
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
