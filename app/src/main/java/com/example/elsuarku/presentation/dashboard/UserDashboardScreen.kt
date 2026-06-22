package com.example.elsuarku.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.presentation.components.GlassCard
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
    onNavigateToElectionList: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Tick every second so countdown timers stay live
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadDashboard(); viewModel.refreshSession() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.HowToVote, null, tint = Gold, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("ElSuarKu", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMyVoting) {
                        Icon(Icons.Filled.HowToVote, "Riwayat Voting", tint = Gold)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Filled.Person, "Profil", tint = OnDeepBlue.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Pengaturan", tint = OnDeepBlue.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Keluar", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && state.activeElections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memuat dashboard...")
            }
        } else if (state.error != null && state.activeElections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Gagal memuat dashboard", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadDashboard(forceReload = true) }) { Text("Coba Lagi") }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftWhite)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── WELCOME BANNER ──
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(DeepBlue, DeepBlueDark)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WavingHand, null, tint = Gold, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Selamat Datang!",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = OnDeepBlue
                                )
                            }
                            if (state.userName.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    state.userName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Gold
                                )
                            }
                            if (state.userEmail.isNotBlank()) {
                                Text(
                                    state.userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnDeepBlue.copy(alpha = 0.55f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Gunakan hak suara Anda dengan bijak. Setiap suara menentukan masa depan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnDeepBlue.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                InfoChip(Icons.Filled.CheckCircle, "${state.votedElectionIds.size} sudah memilih")
                                InfoChip(Icons.Filled.HowToVote, "${state.activeElections.size} pemilihan aktif")
                            }
                        }
                    }
                }

                // ── QUICK ACTIONS ──
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("Lihat\nKandidat", Icons.Filled.People, DeepBlue, Modifier.weight(1f), onClick = {
                            if (state.activeElections.isNotEmpty()) onNavigateToCandidates(state.activeElections.first().id)
                            else onNavigateToElectionList()
                        })
                        QuickAction("Riwayat\nVoting", Icons.Filled.Receipt, EmeraldGreen, Modifier.weight(1f), onClick = onNavigateToMyVoting)
                        QuickAction("Profil\nSaya", Icons.Filled.Person, Gold, Modifier.weight(1f), onClick = onNavigateToProfile)
                    }
                }

                // ── ACTIVE ELECTIONS HEADER ──
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlue, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Pemilihan Aktif",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = DeepBlueDark
                            )
                        }
                        TextButton(onClick = onNavigateToElectionList) {
                            Text("Lihat Semua", style = MaterialTheme.typography.labelMedium, color = DeepBlue)
                        }
                    }
                }

                if (state.activeElections.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.HowToVote, null, tint = DeepBlueLight, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Belum ada pemilihan aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Text("Admin akan segera membuat pemilihan. Silakan cek kembali nanti.", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                        }
                    }
                } else {
                    items(state.activeElections, key = { it.id }) { election ->
                        val hasVoted = election.id in state.votedElectionIds
                        ElectionListItem(
                            election = election,
                            hasVoted = hasVoted,
                            currentTime = currentTime,
                            onClick = { onNavigateToCandidates(election.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ElectionListItem(
    election: Election,
    hasVoted: Boolean,
    currentTime: Long,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        isDark = false,
        onClick = onClick
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Status icon box
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasVoted) EmeraldGreen.copy(alpha = 0.15f)
                        else Gold.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasVoted) Icons.Filled.CheckCircle else Icons.Filled.HowToVote,
                    null,
                    tint = if (hasVoted) EmeraldGreen else Gold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    election.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DeepBlueDark
                )
                Text(
                    election.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBlueLight,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ongoing = currentTime in election.startDate..election.endDate
                    Icon(
                        Icons.Filled.Schedule, null,
                        tint = if (ongoing) EmeraldGreen else StatusError,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (ongoing) election.timeRemainingText(currentTime) else "Berakhir",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ongoing) EmeraldGreen else StatusError
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.People, null, tint = DeepBlueLight, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${election.votedCount}/${election.totalVoters}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepBlueLight
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Vote badge
            Surface(
                color = if (hasVoted) EmeraldGreen.copy(alpha = 0.12f) else Gold.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (hasVoted) "SUDAH\nMEMILIH" else "PILIH",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (hasVoted) EmeraldGreen else Gold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier, cornerRadius = 16.dp, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = OnDeepBlue.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = OnDeepBlue)
        }
    }
}

private fun Election.timeRemainingText(now: Long): String {
    val r = endDate - now
    if (r <= 0) return "Berakhir"
    val d = r / 86400000
    val h = (r % 86400000) / 3600000
    return if (d > 0) "$d hari $h jam" else "$h jam lagi"
}
