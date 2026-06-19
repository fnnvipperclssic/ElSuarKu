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
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCandidates: (String) -> Unit,
    onNavigateToMyVoting: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard(); viewModel.refreshSession() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.HowToVote, null, tint = Gold, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("ElSuarKu", color = OnDeepBlue, fontWeight = FontWeight.Bold) } },
                actions = {
                    IconButton(onClick = onNavigateToMyVoting) { Icon(Icons.Filled.HowToVote, "Riwayat Voting", tint = Gold) }
                    IconButton(onClick = onNavigateToProfile) { Icon(Icons.Filled.Person, "Profil", tint = OnDeepBlue.copy(alpha = 0.7f)) }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Filled.Settings, "Pengaturan", tint = OnDeepBlue.copy(alpha = 0.7f)) }
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, "Keluar", tint = Gold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator(message = "Memuat dashboard...") }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Welcome Banner
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlue), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.WavingHand, null, tint = Gold, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(8.dp)); Text("Selamat Datang!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OnDeepBlue) }
                            Spacer(Modifier.height(6.dp))
                            Text("Gunakan hak suara Anda dengan bijak. Setiap suara menentukan masa depan.", style = MaterialTheme.typography.bodyMedium, color = OnDeepBlue.copy(alpha = 0.8f))
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                InfoChip(Icons.Filled.CheckCircle, "${state.votedElectionIds.size} sudah memilih")
                                InfoChip(Icons.Filled.HowToVote, "${state.activeElections.size} pemilihan aktif")
                            }
                        }
                    }
                }

                // Quick Actions
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("Lihat\nKandidat", Icons.Filled.People, DeepBlue, Modifier.weight(1f), onClick = onNavigateToMyVoting)
                        QuickAction("Riwayat\nVoting", Icons.Filled.Receipt, EmeraldGreen, Modifier.weight(1f), onClick = onNavigateToMyVoting)
                        QuickAction("Profil\nSaya", Icons.Filled.Person, Gold, Modifier.weight(1f), onClick = onNavigateToProfile)
                    }
                }

                // Active Elections
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Ballot, null, tint = DeepBlue, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Pemilihan Aktif", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
                }

                if (state.activeElections.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueSurface), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.HowToVote, null, tint = DeepBlueLight, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("Belum ada pemilihan aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Text("Admin akan segera membuat pemilihan. Silakan cek kembali nanti.", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                        }
                    }
                } else {
                    items(state.activeElections, key = { it.id }) { election ->
                        val hasVoted = election.id in state.votedElectionIds
                        Card(
                            onClick = { onNavigateToCandidates(election.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (hasVoted) EmeraldGreenSurface else SurfaceWhite),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Status icon
                                Box(Modifier.size(48.dp).background(if (hasVoted) EmeraldGreen.copy(alpha = 0.2f) else Gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Icon(if (hasVoted) Icons.Filled.CheckCircle else Icons.Filled.HowToVote, null, tint = if (hasVoted) EmeraldGreen else Gold, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(election.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                    Text(election.description, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight, maxLines = 2)
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Schedule, null, tint = if (election.isOngoing()) EmeraldGreen else StatusError, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (election.isOngoing()) election.timeRemainingText() else "Berakhir", style = MaterialTheme.typography.labelSmall, color = if (election.isOngoing()) EmeraldGreen else StatusError)
                                        Spacer(Modifier.width(12.dp))
                                        Icon(Icons.Filled.People, null, tint = DeepBlueLight, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${election.votedCount}/${election.totalVoters}", style = MaterialTheme.typography.labelSmall, color = DeepBlueLight)
                                    }
                                }
                                Surface(color = if (hasVoted) EmeraldGreen.copy(alpha = 0.15f) else Gold.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                                    Text(if (hasVoted) "SUDAH MEMILIH" else "PILIH", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (hasVoted) EmeraldGreen else Gold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(6.dp)); Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color) }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(color = OnDeepBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Gold, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(text, style = MaterialTheme.typography.labelSmall, color = OnDeepBlue) }
    }
}

private fun Election.isOngoing() = System.currentTimeMillis() in startDate..endDate
private fun Election.timeRemainingText(): String { val r = endDate - System.currentTimeMillis(); if (r <= 0) return "Berakhir"; val d = r / 86400000; val h = (r % 86400000) / 3600000; return if (d > 0) "$d hari $h jam" else "$h jam lagi" }
