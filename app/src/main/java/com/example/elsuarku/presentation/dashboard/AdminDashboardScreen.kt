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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.SeedDataDemo
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.presentation.admin.AdminViewModel
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateToElections: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var seedMsg by remember { mutableStateOf("") }
    var isSeeding by remember { mutableStateOf(false) }

    LaunchedEffect(state.elections) {
        if (state.elections.isEmpty() && !state.isLoading && !isSeeding) {
            isSeeding = true
            seedMsg = SeedDataDemo.seed(viewModel.userId)
            viewModel.loadAllData()
            isSeeding = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AdminPanelSettings, null, tint = Gold, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Panel Administrator", color = OnDeepBlue, fontWeight = FontWeight.Bold) } },
                actions = {
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Filled.Settings, null, tint = OnDeepBlue.copy(alpha = 0.7f)) }
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, null, tint = Gold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlueDark)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ===== HEADER BADGE =====
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlue), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Shield, null, tint = Gold, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("ADMINISTRATOR", style = MaterialTheme.typography.labelLarge, color = Gold, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(4.dp))
                        Text("Kendali penuh: kelola pemilihan, kandidat, pengguna, laporan, dan keamanan sistem.", style = MaterialTheme.typography.bodyMedium, color = OnDeepBlue.copy(alpha = 0.8f))
                    }
                }
            }

            // ===== STATISTIK UTAMA =====
            item { SectionTitle("Dashboard Utama") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashStat("Pemilihan", state.totalElections, Icons.Filled.Ballot, DeepBlueLight, Modifier.weight(1f))
                    DashStat("Kandidat", state.totalCandidates, Icons.Filled.People, EmeraldGreen, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashStat("Pemilih", state.totalVoters, Icons.Filled.Group, Gold, Modifier.weight(1f))
                    DashStat("Suara Masuk", state.totalVotes, Icons.Filled.HowToVote, DeepBlue, Modifier.weight(1f))
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = EmeraldGreenSurface), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Partisipasi", style = MaterialTheme.typography.labelMedium, color = EmeraldGreen)
                        Text("${"%.1f".format(state.participationRate)}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        LinearProgressIndicator(progress = { state.participationRate / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = EmeraldGreen, trackColor = EmeraldGreen.copy(alpha = 0.1f))
                    }
                }
            }

            // ===== MENU UTAMA =====
            item { SectionTitle("Menu Administrasi") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMenuCard("Pemilihan", "Buat, edit, aktifkan,\nnonaktifkan pemilihan", Icons.Filled.Ballot, EmeraldGreen, Modifier.weight(1f), onNavigateToElections)
                    AdminMenuCard("Kandidat", "Tambah, edit, hapus\nkandidat & foto", Icons.Filled.PersonAdd, DeepBlueLight, Modifier.weight(1f), onNavigateToElections)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMenuCard("Pengguna", "Kelola role, suspend,\naktifkan akun", Icons.Filled.ManageAccounts, Gold, Modifier.weight(1f), onNavigateToUserManagement)
                    AdminMenuCard("Laporan", "Generate & export\nhasil pemilihan", Icons.Filled.Description, DeepBlue, Modifier.weight(1f), onNavigateToReport)
                }
            }
            item {
                AdminMenuCard("Keamanan", "Log aktivitas, alert,\nblacklist device", Icons.Filled.Security, StatusError, Modifier.fillMaxWidth(), onNavigateToSecurity)
            }

            // ===== DAFTAR PEMILIHAN =====
            item { SectionTitle("Pemilihan Aktif") }
            if (state.elections.isEmpty() && !state.isLoading) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueSurface)) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Belum ada pemilihan", style = MaterialTheme.typography.titleMedium, color = DeepBlueDark)
                            Text("Data demo akan dibuat otomatis", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            if (seedMsg.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(seedMsg, style = MaterialTheme.typography.bodySmall, color = EmeraldGreen, textAlign = TextAlign.Center) }
                        }
                    }
                }
            } else {
                items(state.elections.take(5)) { election ->
                    Card(onClick = onNavigateToElections, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlue, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(election.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Text("${election.votedCount}/${election.totalVoters} suara | ${election.status.name}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                            Surface(color = when (election.status) { ElectionStatus.ACTIVE -> EmeraldGreen.copy(alpha = 0.15f); ElectionStatus.COMPLETED -> DeepBlueLight.copy(alpha = 0.15f); else -> Gold.copy(alpha = 0.15f) }, shape = RoundedCornerShape(6.dp)) { Text(election.status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = when (election.status) { ElectionStatus.ACTIVE -> EmeraldGreen; ElectionStatus.COMPLETED -> DeepBlueLight; else -> Gold }) }
                        }
                    }
                }
            }

            // ===== SECURITY ALERTS =====
            if (state.securityAlerts.isNotEmpty()) {
                item { SectionTitle("Peringatan Keamanan (${state.securityAlerts.size})") }
                items(state.securityAlerts.take(3)) { alert ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.05f)), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = StatusError, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) { Text(alert.action.name.replace("_", " "), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = StatusError); Text("${alert.actorName} — ${alert.targetName}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark)
}

@Composable
private fun DashStat(label: String, value: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun AdminMenuCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), lineHeight = MaterialTheme.typography.labelSmall.lineHeight)
        }
    }
}
