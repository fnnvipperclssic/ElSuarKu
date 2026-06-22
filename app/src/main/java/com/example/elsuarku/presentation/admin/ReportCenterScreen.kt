package com.example.elsuarku.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*
import com.example.elsuarku.utils.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCenterScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var selectedElection by remember { mutableStateOf<Election?>(null) }
    var reportText by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadAllData() }

    // Load candidates when election selected for report generation
    LaunchedEffect(selectedElection) {
        selectedElection?.let { viewModel.loadCandidates(it.id) }
    }

    if (state.isLoading && state.elections.isEmpty()) {
        Box(Modifier.fillMaxSize().background(SoftWhite), contentAlignment = Alignment.Center) {
            LoadingIndicator(message = "Memuat data...")
        }
        return
    }

    if (state.error != null && state.elections.isEmpty()) {
        Box(Modifier.fillMaxSize().background(SoftWhite), contentAlignment = Alignment.Center) {
            GlassCard(modifier = Modifier.padding(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Gagal memuat data", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                    Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.loadAllData() }) { Text("Coba Lagi") }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Center", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (showReport && reportText.isNotBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding).padding(16.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Hasil Laporan", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = DeepBlueDark)
                        Row {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(reportText))
                                context.showToast("Disalin ke clipboard")
                            }) { Icon(Icons.Filled.ContentCopy, "Copy", tint = DeepBlue) }
                            IconButton(onClick = { showReport = false }) {
                                Icon(Icons.Filled.Close, "Tutup", tint = StatusError)
                            }
                        }
                    }
                }
                item { Text(reportText, style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark) }
                item {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showReport = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Kembali") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Export Laporan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = DeepBlueDark)
                }
                item {
                    Text("Pilih pemilihan untuk generate laporan:", style = MaterialTheme.typography.bodyMedium, color = DeepBlueLight)
                }

                if (state.elections.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Description, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Belum ada pemilihan", color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                                Text("Buat pemilihan terlebih dahulu", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                        }
                    }
                } else {
                    items(state.elections) { election ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            isDark = false,
                            onClick = { selectedElection = election }
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Ballot, null, tint = DeepBlue, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(election.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = DeepBlueDark)
                                    Text("${election.votedCount}/${election.totalVoters} suara | ${election.status.name}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                }
                                if (selectedElection == election) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                    if (selectedElection != null) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        reportText = viewModel.exportReport(selectedElection!!.id)
                                        showReport = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Filled.Description, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Generate Report", style = MaterialTheme.typography.labelLarge)
                                }
                                Button(
                                    onClick = {
                                        reportText = viewModel.exportReport(selectedElection!!.id)
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Laporan: ${selectedElection!!.title}")
                                            putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Laporan"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Bagikan", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Format Export", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DeepBlueDark)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportFormatCard("TXT", "Plain Text", Icons.AutoMirrored.Filled.TextSnippet, DeepBlueLight, Modifier.weight(1f), onClick = {
                            if (selectedElection != null) {
                                reportText = viewModel.exportReport(selectedElection!!.id)
                                showReport = true
                            }
                        })
                        ExportFormatCard("CSV", "Spreadsheet", Icons.Filled.TableChart, EmeraldGreen, Modifier.weight(1f), onClick = {
                            if (selectedElection != null) {
                                reportText = viewModel.exportReport(selectedElection!!.id)
                                    .replace("=", ",").replace(": ", ",")
                                showReport = true
                            }
                        })
                        ExportFormatCard("PDF*", "Document", Icons.Filled.PictureAsPdf, StatusError, Modifier.weight(1f), onClick = {
                            context.showToast("Export PDF belum tersedia — gunakan TXT atau CSV")
                        })
                    }
                    Text("* PDF belum tersedia. TXT & CSV dapat langsung digunakan.", style = MaterialTheme.typography.labelSmall, color = DeepBlueLight)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    label: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit = {}
) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}

