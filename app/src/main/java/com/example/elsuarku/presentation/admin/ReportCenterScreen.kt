package com.example.elsuarku.presentation.admin

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCenterScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var selectedElection by remember { mutableStateOf<Election?>(null) }
    var reportText by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadAllData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Center", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (showReport && reportText.isNotBlank()) {
            LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding).padding(16.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hasil Laporan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DeepBlueDark)
                        Row {
                            IconButton(onClick = { clipboard.setText(AnnotatedString(reportText)); context.showToast("Disalin ke clipboard") }) { Icon(Icons.Filled.ContentCopy, "Copy", tint = DeepBlue) }
                            IconButton(onClick = { showReport = false }) { Icon(Icons.Filled.Close, "Tutup", tint = StatusError) }
                        }
                    }
                }
                item { Text(reportText, style = MaterialTheme.typography.bodyMedium, color = DeepBlueDark) }
                item {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showReport = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepBlue), shape = RoundedCornerShape(12.dp)) { Text("Kembali") }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Export Laporan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
                item { Text("Pilih pemilihan untuk generate laporan:", style = MaterialTheme.typography.bodyMedium, color = DeepBlueLight) }

                if (state.elections.isEmpty()) {
                    item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueSurface)) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Description, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(8.dp)); Text("Belum ada pemilihan", color = DeepBlueDark); Text("Buat pemilihan terlebih dahulu", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) } } }
                } else {
                    items(state.elections) { election ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (selectedElection == election) EmeraldGreenSurface else SurfaceWhite), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp), onClick = { selectedElection = election; viewModel.loadCandidates(election.id) }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Ballot, null, tint = DeepBlue, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(election.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                    Text("${election.votedCount}/${election.totalVoters} suara | ${election.status.name}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                }
                                if (selectedElection == election) Icon(Icons.Filled.CheckCircle, null, tint = EmeraldGreen)
                            }
                        }
                    }

                    if (selectedElection != null) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { reportText = viewModel.exportReport(selectedElection!!.id); showReport = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Filled.Description, null); Spacer(Modifier.width(4.dp)); Text("Generate Report") }
                            }
                        }
                    }
                }

                // Export format info
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Format Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportFormatCard("TXT", "Plain Text", Icons.Filled.TextSnippet, DeepBlueLight, Modifier.weight(1f))
                        ExportFormatCard("CSV", "Spreadsheet", Icons.Filled.TableChart, EmeraldGreen, Modifier.weight(1f))
                        ExportFormatCard("PDF*", "Document", Icons.Filled.PictureAsPdf, StatusError, Modifier.weight(1f))
                    }
                    Text("* PDF memerlukan library tambahan", style = MaterialTheme.typography.labelSmall, color = DeepBlueLight)
                }
            }
        }
    }
}

@Composable
private fun ExportFormatCard(label: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

private fun android.content.Context.showToast(msg: String) = android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
