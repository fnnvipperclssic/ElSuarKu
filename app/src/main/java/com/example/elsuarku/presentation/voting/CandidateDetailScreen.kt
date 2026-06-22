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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.CandidateCard
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidateDetailScreen(
    electionId: String,
    viewModel: VotingViewModel,
    onVoteClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.candidateListState.collectAsState()

    LaunchedEffect(electionId) { viewModel.loadCandidates(electionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.election?.title ?: "Kandidat",
                        color = OnDeepBlue,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
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
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memuat kandidat…")
            }
        } else if (state.error != null && state.candidates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Gagal memuat kandidat", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadCandidates(electionId) }) { Text("Coba Lagi") }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = DeepBlueSurface,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = state.election?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepBlueDark
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Pilih satu kandidat di bawah ini:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = DeepBlueLight
                            )
                        }
                    }
                }

                if (state.candidates.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.People, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada kandidat.", style = MaterialTheme.typography.bodyMedium, color = DeepBlueLight)
                            }
                        }
                    }
                } else {
                    items(state.candidates) { candidate ->
                        CandidateCard(candidate = candidate, onClick = { onVoteClick(candidate.id) })
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
