package com.example.elsuarku.presentation.dashboard

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
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVotingScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Riwayat Voting Saya", color = OnDeepBlue, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnDeepBlue) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue))
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = EmeraldGreenSurface), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.HowToVote, null, tint = EmeraldGreen, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Status Voting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen) }
                        Spacer(Modifier.height(8.dp))
                        Text("Anda telah memilih di ${state.votedElectionIds.size} pemilihan", style = MaterialTheme.typography.bodyLarge, color = EmeraldGreenDark)
                    }
                }
            }

            if (state.activeElections.isEmpty()) {
                item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueSurface)) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Info, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(8.dp)); Text("Belum ada aktivitas voting", color = DeepBlueDark); Text("Ikuti pemilihan yang tersedia", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) } } }
            } else {
                items(state.activeElections) { election ->
                    val hasVoted = election.id in state.votedElectionIds
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (hasVoted) EmeraldGreenSurface else SurfaceWhite), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (hasVoted) Icons.Filled.CheckCircle else Icons.Filled.Pending, null, tint = if (hasVoted) EmeraldGreen else Gold, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(election.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Text(if (hasVoted) "SUDAH MEMILIH" else "Belum memilih", style = MaterialTheme.typography.labelMedium, color = if (hasVoted) EmeraldGreen else Gold)
                            }
                        }
                    }
                }
            }
        }
    }
}
