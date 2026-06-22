package com.example.elsuarku.presentation.monitor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.PulseDot
import com.example.elsuarku.presentation.components.StatCard
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue
import com.example.elsuarku.ui.theme.SoftWhite
import com.example.elsuarku.ui.theme.StatusError

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

    // Auto-refresh every 30s while screen is visible
    LaunchedEffect(electionId) {
        while (true) {
            delay(30_000L)
            viewModel.refresh()
        }
    }

    val election = state.selectedElection
    val totalVoters = election?.totalVoters ?: 0
    val votedCount = state.electionVoteCount
    val participationRate = if (totalVoters > 0) {
        (votedCount.toFloat() / totalVoters * 100)
    } else 0f

    val animatedParticipation by animateFloatAsState(
        targetValue = participationRate,
        animationSpec = tween(800)
    )

    val hasCriticalAlerts = state.securityAlerts.any { it.severity == AuditSeverity.CRITICAL }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = OnDeepBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Live Statistics", color = OnDeepBlue, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                actions = {
                    if (hasCriticalAlerts) {
                        PulseDot(color = StatusError, size = 8)
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && election == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                com.example.elsuarku.presentation.components.LoadingIndicator(message = "Memuat statistik...")
            }
            return@Scaffold
        }

        if (state.error != null && election == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Gagal memuat statistik", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                        Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadElectionStats(electionId) }) { Text("Coba Lagi") }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Election title
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = DeepBlue.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = DeepBlue, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = election?.title ?: "Memuat data...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepBlueDark
                            )
                            Text(
                                text = "Live monitoring partisipasi pemilih",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepBlueDark.copy(alpha = 0.55f)
                            )
                        }
                    }
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
                val rateColor = when {
                    participationRate > 75 -> EmeraldGreen
                    participationRate > 50 -> Gold
                    else -> DeepBlue
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tingkat Partisipasi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DeepBlueDark
                        )
                        Surface(
                            color = rateColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "%.1f%%".format(animatedParticipation),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = rateColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (animatedParticipation / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = rateColor,
                        trackColor = DeepBlue.copy(alpha = 0.08f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
            item {
                Text(
                    "Distribusi Suara",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kandidat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepBlueLight)
                            Text("Suara", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DeepBlueLight)
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = DeepBlue.copy(alpha = 0.08f))
                        Spacer(Modifier.height(8.dp))
                        if (state.candidates.isEmpty()) {
                            Text(
                                if (state.isLoading) "Memuat data kandidat..."
                                else "Data kandidat akan tampil setelah Admin menambahkan kandidat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepBlueLight
                            )
                        } else {
                            state.candidates.sortedByDescending { it.voteCount }.forEach { candidate ->
                                val badgeColor = when (candidate.nomorUrut) {
                                    1 -> Gold
                                    2 -> DeepBlue
                                    3 -> EmeraldGreen
                                    else -> DeepBlueLight
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            modifier = Modifier.size(30.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeColor.copy(alpha = 0.12f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "${candidate.nomorUrut}",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = badgeColor
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(candidate.name, style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark)
                                    }
                                    Surface(
                                        color = EmeraldGreen.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "${candidate.voteCount} suara",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreen
                                        )
                                    }
                                }
                                if (candidate != state.candidates.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = DeepBlue.copy(alpha = 0.06f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Alert summary — system-wide (not election-specific)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasCriticalAlerts) {
                                PulseDot(color = StatusError, size = 8)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Peringatan Keamanan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = DeepBlueDark)
                        }
                        Surface(
                            color = if (state.securityAlerts.isEmpty()) EmeraldGreen.copy(alpha = 0.1f) else StatusError.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (state.securityAlerts.isEmpty()) "AMAN" else "${state.securityAlerts.size} alert",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (state.securityAlerts.isEmpty()) EmeraldGreen else StatusError
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Seluruh sistem • bukan spesifik pemilihan ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepBlueLight.copy(alpha = 0.45f)
                    )
                }
            }

            // Refresh button
            item {
                Button(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Data", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}
