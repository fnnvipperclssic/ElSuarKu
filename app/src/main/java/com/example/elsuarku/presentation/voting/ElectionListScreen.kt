package com.example.elsuarku.presentation.voting

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
import com.example.elsuarku.presentation.components.ElectionCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectionListScreen(viewModel: VotingViewModel, onCandidateClick: (String) -> Unit, onBack: () -> Unit) {
    val state by viewModel.electionListState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadElections() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Ballot, null, tint = Gold, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Pemilihan Aktif", color = OnDeepBlue, fontWeight = FontWeight.Bold) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue))
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator(message = "Memuat pemilihan…") }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (state.elections.isNotEmpty()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = EmeraldGreenSurface), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Info, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("${state.elections.size} pemilihan tersedia — pilih untuk melihat kandidat", style = MaterialTheme.typography.bodySmall, color = EmeraldGreenDark) }
                        }
                    }
                }
                if (state.elections.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueSurface), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(56.dp)); Spacer(Modifier.height(10.dp)); Text("Belum ada pemilihan aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DeepBlueDark); Text("Admin akan membuat pemilihan segera.", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) }
                        }
                    }
                } else {
                    items(state.elections) { election -> ElectionCard(election = election, onClick = { onCandidateClick(election.id) }) }
                }
            }
        }
    }
}
