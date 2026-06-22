package com.example.elsuarku.presentation.voting

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.ElectionCard
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectionListScreen(
    viewModel: VotingViewModel,
    onCandidateClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.electionListState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadElections() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Ballot, null, tint = Gold, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Pemilihan Aktif", color = OnDeepBlue, fontWeight = FontWeight.Bold)
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
        if (state.isLoading && state.elections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memuat pemilihan…")
            }
        } else if (state.error != null && state.elections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Gagal memuat pemilihan", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadElections() }) { Text("Coba Lagi") }
                    }
                }
            }
        } else if (state.error != null) {
            // Error on refresh but stale data exists — show banner at top
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = StatusError.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = StatusError, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gagal refresh: ${state.error}", style = MaterialTheme.typography.bodySmall, color = StatusError, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.loadElections() }) { Text("Coba Lagi", color = DeepBlue) }
                        }
                    }
                }
                item {
                    Surface(Modifier.fillMaxWidth(), color = EmeraldGreen.copy(alpha = 0.08f), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("${state.elections.size} pemilihan tersedia — pilih untuk melihat kandidat", style = MaterialTheme.typography.bodySmall, color = EmeraldGreenDark)
                        }
                    }
                }
                items(state.elections) { election ->
                    ElectionCard(election = election, onClick = { onCandidateClick(election.id) })
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            return@Scaffold
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (state.elections.isNotEmpty()) {
                        Surface(
                            Modifier.fillMaxWidth(),
                            color = EmeraldGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${state.elections.size} pemilihan tersedia — pilih untuk melihat kandidat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldGreenDark
                                )
                            }
                        }
                    }
                }
                if (state.elections.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Belum ada pemilihan aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Text("Admin akan membuat pemilihan segera.", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                        }
                    }
                } else {
                    items(state.elections) { election ->
                        ElectionCard(election = election, onClick = { onCandidateClick(election.id) })
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
