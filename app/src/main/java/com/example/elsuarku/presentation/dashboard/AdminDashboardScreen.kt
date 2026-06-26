package com.example.elsuarku.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.presentation.admin.AdminViewModel
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.PulseDot
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

    // Trigger data loading when screen first composes.
    // ViewModel.init does NOT load data — it only sets identity info.
    // Subsequent calls are no-ops (observers already running).
    LaunchedEffect(Unit) { viewModel.loadAllData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AdminPanelSettings, null,
                            tint = Gold, modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Panel Administrator", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, null, tint = OnDeepBlue.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && state.elections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memuat dashboard administrator...")
            }
            return@Scaffold
        }

        if (state.error != null && state.elections.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Gagal memuat dashboard", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadAllData() }) { Text("Coba Lagi") }
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftWhite)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── HEADER BADGE ──
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
                            Icon(Icons.Filled.Shield, null, tint = Gold, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ADMINISTRATOR",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Gold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        if (state.adminName.isNotBlank()) {
                            Text(
                                "Login sebagai: ${state.adminName}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnDeepBlue.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Kendali penuh: kelola pemilihan, kandidat, pengguna, laporan, dan keamanan sistem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnDeepBlue.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── STATISTIK UTAMA ──
            item {
                Text(
                    "Dashboard Utama",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashStat("Pemilihan", state.totalElections, Icons.Filled.Ballot, DeepBlueLight, Modifier.weight(1f))
                    DashStat("Kandidat", state.totalCandidates, Icons.Filled.People, EmeraldGreen, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashStat("Pengguna", state.totalVoters, Icons.Filled.Group, Gold, Modifier.weight(1f))
                    DashStat("Suara Masuk", state.totalVotes, Icons.Filled.HowToVote, DeepBlue, Modifier.weight(1f))
                }
            }

            // ── PARTISIPASI ──
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Partisipasi",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldGreen.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${"%.1f".format(state.participationRate)}%",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreen
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp, null,
                            tint = EmeraldGreen.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (state.participationRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldGreen,
                        trackColor = EmeraldGreen.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // ── MENU ADMINISTRASI ──
            item {
                Text(
                    "Menu Administrasi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMenuCard("Pemilihan", "Buat, edit, aktifkan,\nnonaktifkan pemilihan", Icons.Filled.Ballot, EmeraldGreen, Modifier.weight(1f), onNavigateToElections)
                    AdminMenuCard("Kandidat", "Tambah, edit, hapus\nkandidat & foto", Icons.Filled.PersonAdd, DeepBlueLight, Modifier.weight(1f), onNavigateToElections)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMenuCard("Pengguna", "Kelola role, suspend,\naktifkan akun", Icons.Filled.ManageAccounts, Gold, Modifier.weight(1f), onNavigateToUserManagement)
                    AdminMenuCard("Laporan", "Generate & export\nhasil pemilihan", Icons.Filled.Description, DeepBlue, Modifier.weight(1f), onNavigateToReport)
                }
            }
            item {
                AdminMenuCard("Keamanan", "Log aktivitas, alert,\nblacklist device", Icons.Filled.Security, StatusError, Modifier.fillMaxWidth(), onNavigateToSecurity)
            }

            // ── SEMUA PEMILIHAN ──
            item {
                Text(
                    "Semua Pemilihan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
            if (state.elections.isEmpty() && !state.isLoading) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Belum ada pemilihan", style = MaterialTheme.typography.titleMedium, color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                            Text("Buat pemilihan baru melalui menu Pemilihan", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        }
                    }
                }
            } else {
                items(state.elections) { election ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (election.status) {
                                            ElectionStatus.ACTIVE -> EmeraldGreen.copy(alpha = 0.15f)
                                            ElectionStatus.COMPLETED -> DeepBlueLight.copy(alpha = 0.15f)
                                            else -> Gold.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Ballot, null,
                                    tint = when (election.status) {
                                        ElectionStatus.ACTIVE -> EmeraldGreen
                                        ElectionStatus.COMPLETED -> DeepBlueLight
                                        else -> Gold
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(election.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Text(
                                    "${election.votedCount}/${election.totalVoters} suara • ${election.status.name}",
                                    style = MaterialTheme.typography.bodySmall, color = DeepBlueLight
                                )
                            }
                            Surface(
                                color = when (election.status) {
                                    ElectionStatus.ACTIVE -> EmeraldGreen.copy(alpha = 0.12f)
                                    ElectionStatus.COMPLETED -> DeepBlueLight.copy(alpha = 0.12f)
                                    else -> Gold.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    when (election.status) {
                                        ElectionStatus.ACTIVE -> "AKTIF"
                                        ElectionStatus.DRAFT -> "DRAFT"
                                        ElectionStatus.COMPLETED -> "SELESAI"
                                        ElectionStatus.CANCELLED -> "BATAL"
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (election.status) {
                                        ElectionStatus.ACTIVE -> EmeraldGreen
                                        ElectionStatus.COMPLETED -> DeepBlueLight
                                        else -> Gold
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── SECURITY ALERTS ──
            if (state.securityAlerts.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseDot(color = StatusError, size = 8)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Peringatan Keamanan (${state.securityAlerts.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusError
                        )
                    }
                }
                items(state.securityAlerts.take(3)) { alert ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = StatusError, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    alert.action.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = StatusError
                                )
                                Text(
                                    "${alert.actorName} — ${alert.targetName}",
                                    style = MaterialTheme.typography.bodySmall, color = DeepBlueLight
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun DashStat(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            "$value",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun AdminMenuCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier, cornerRadius = 16.dp, onClick = onClick) {
        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
        Spacer(Modifier.height(4.dp))
        Text(
            desc,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            lineHeight = MaterialTheme.typography.labelSmall.lineHeight
        )
    }
}
