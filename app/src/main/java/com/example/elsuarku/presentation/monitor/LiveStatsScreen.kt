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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.StatCard
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue
import com.example.elsuarku.ui.theme.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStatsScreen(
    electionId: String,
    viewModel: MonitorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(electionId) {
        viewModel.loadElectionStats(electionId)
    }

    val election = state.selectedElection
    val totalVoters = election?.totalVoters ?: 0
    val votedCount = state.electionVoteCount
    val participationRate = if (totalVoters > 0) {
        (votedCount.toFloat() / totalVoters * 100)
    } else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Statistics", color = OnDeepBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftWhite)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Election title
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = election?.title ?: "",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Monitoring real-time partisipasi pemilih",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueDark.copy(alpha = 0.6f)
                    )
                }
            }

            // Key metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Pemilih",
                        value = "$totalVoters",
                        icon = Icons.Filled.People,
                        accentColor = DeepBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Sudah Memilih",
                        value = "$votedCount",
                        icon = Icons.Filled.HowToVote,
                        accentColor = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Participation rate
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tingkat Partisipasi",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepBlueDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${"%.1f".format(participationRate)}%",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            participationRate > 75 -> EmeraldGreen
                            participationRate > 50 -> Gold
                            else -> DeepBlueDark
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { participationRate / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = when {
                            participationRate > 75 -> EmeraldGreen
                            participationRate > 50 -> Gold
                            else -> DeepBlue
                        },
                        trackColor = DeepBlue.copy(alpha = 0.1f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$votedCount dari $totalVoters pemilih",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepBlueDark.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${totalVoters - votedCount} belum memilih",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepBlueDark.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Candidate vote breakdown
            item { Text("Distribusi Suara", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
            // Note: Real-time candidate list requires observeCandidates from repository
            // For now show summary
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kandidat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepBlueLight)
                            Text("Suara", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepBlueLight)
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text("Data kandidat akan tampil setelah Admin menambahkan kandidat.", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                    }
                }
            }

            // System Health
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status Sistem", style = MaterialTheme.typography.titleMedium, color = DeepBlueDark)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(EmeraldGreen, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("ONLINE", style = MaterialTheme.typography.labelLarge, color = EmeraldGreen)
                        }
                    }
                }
            }

            // Refresh
            item {
                Button(onClick = { viewModel.refresh() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepBlue), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Refresh Data")
                }
            }
        }
    }
}
