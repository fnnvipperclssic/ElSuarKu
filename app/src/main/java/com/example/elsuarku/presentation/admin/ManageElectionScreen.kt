package com.example.elsuarku.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageElectionScreen(
    viewModel: AdminViewModel,
    onManageCandidates: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Election?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Election?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadAllData() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Pemilihan", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(Icons.Filled.Add, "Tambah Pemilihan", tint = Gold, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DeepBlue)
                    }
                }
            } else if (state.error != null && state.elections.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Gagal memuat pemilihan", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                            Text(state.error ?: "Error tidak diketahui", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.loadAllData() }) { Text("Coba Lagi") }
                        }
                    }
                }
            } else if (state.elections.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Belum ada pemilihan", style = MaterialTheme.typography.titleMedium, color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                            Text("Klik + untuk membuat pemilihan pertama", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                        }
                    }
                }
            } else {
                items(state.elections, key = { it.id }) { election ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Ballot, null, tint = DeepBlue, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(election.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = DeepBlueDark)
                                Text(election.description, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight, maxLines = 1)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = when (election.status) {
                                    ElectionStatus.ACTIVE -> EmeraldGreen.copy(alpha = 0.12f)
                                    ElectionStatus.DRAFT -> Gold.copy(alpha = 0.12f)
                                    ElectionStatus.COMPLETED -> DeepBlueLight.copy(alpha = 0.12f)
                                    else -> StatusError.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    election.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (election.status) {
                                        ElectionStatus.ACTIVE -> EmeraldGreen
                                        ElectionStatus.DRAFT -> Gold
                                        ElectionStatus.COMPLETED -> DeepBlueLight
                                        else -> StatusError
                                    }
                                )
                            }
                            Row {
                                IconButton(onClick = { onManageCandidates(election.id) }) {
                                    Icon(Icons.Filled.People, "Kandidat", tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { showEditDialog = election }) {
                                    Icon(Icons.Filled.Edit, "Edit", tint = DeepBlue, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { showDeleteDialog = election }) {
                                    Icon(Icons.Filled.Delete, "Hapus", tint = StatusError, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        ElectionFormSheet(
            title = "Buat Pemilihan Baru",
            election = null,
            onSave = { t, d, s, e -> viewModel.createElection(t, d, s, e); showCreateSheet = false },
            onDismiss = { showCreateSheet = false }
        )
    }

    showEditDialog?.let { election ->
        ElectionFormSheet(
            title = "Edit Pemilihan",
            election = election,
            onSave = { t, d, s, e ->
                viewModel.updateElection(election.id, t, d, s, e)
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
        )
    }

    showDeleteDialog?.let { election ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hapus Pemilihan?", fontWeight = FontWeight.SemiBold) },
            text = { Text("Yakin hapus \"${election.title}\"? Tindakan ini permanen.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteElection(election.id); showDeleteDialog = null }) {
                    Text("Hapus", color = StatusError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Batal") }
            }
        )
    }

    state.error?.let {
        ErrorDialog(message = it, onDismiss = { viewModel.clearMessages() }, onRetry = { viewModel.loadAllData() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElectionFormSheet(
    title: String,
    election: Election?,
    onSave: (String, String, Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var t by remember { mutableStateOf(election?.title ?: "") }
    var d by remember { mutableStateOf(election?.description ?: "") }
    // Preserve existing duration when editing; default to "7" for new elections
    var days by remember {
        mutableStateOf(
            if (election != null && election.startDate > 0 && election.endDate > election.startDate) {
                ((election.endDate - election.startDate) / 86400000L).toString()
            } else "7"
        )
    }
    var durationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = DeepBlue)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = t, onValueChange = { t = it },
                label = { Text("Judul Pemilihan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = d, onValueChange = { d = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = days, onValueChange = { days = it; durationError = null },
                label = { Text("Durasi (hari)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = durationError != null,
                supportingText = durationError?.let { { Text(it, color = StatusError) } }
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    val dur = days.toLongOrNull()
                    if (dur == null || dur <= 0) {
                        durationError = "Durasi harus berupa angka positif (minimal 1 hari)"
                        return@Button
                    }
                    durationError = null
                    // Preserve original startDate on edit; use current time for new elections
                    val start = election?.startDate?.let { if (it > 0) it else System.currentTimeMillis() }
                        ?: System.currentTimeMillis()
                    onSave(t, d, start, start + dur * 86400000L)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = t.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (election == null) "Buat Pemilihan" else "Simpan Perubahan",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
