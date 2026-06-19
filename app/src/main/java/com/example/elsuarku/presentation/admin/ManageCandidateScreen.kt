package com.example.elsuarku.presentation.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.presentation.components.CandidateCard
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCandidateScreen(electionId: String, viewModel: AdminViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Candidate?>(null) }
    val context = LocalContext.current

    LaunchedEffect(electionId) { viewModel.loadCandidates(electionId) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.selectedElection?.title ?: "Kelola Kandidat", color = OnDeepBlue, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue) } }, actions = { IconButton(onClick = { showCreateSheet = true }) { Icon(Icons.Filled.PersonAdd, "Tambah Kandidat", tint = Gold) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue))
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator(message = "Memuat kandidat…") }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.candidates.isEmpty()) {
                    item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepBlueSurface)) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.People, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(8.dp)); Text("Belum ada kandidat", style = MaterialTheme.typography.titleMedium, color = DeepBlueDark); Text("Klik + untuk menambahkan kandidat", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight) } } }
                } else {
                    items(state.candidates, key = { it.id }) { candidate ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Number badge
                                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(22.dp), color = if (candidate.nomorUrut <= 2) Gold.copy(alpha = 0.2f) else DeepBlueLight.copy(alpha = 0.2f)) { Box(contentAlignment = Alignment.Center) { Text("${candidate.nomorUrut}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (candidate.nomorUrut <= 2) Gold else DeepBlueLight) } }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(candidate.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DeepBlueDark)
                                    if (candidate.visi.isNotBlank()) Text(candidate.visi, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight, maxLines = 1)
                                    Text("Suara: ${candidate.voteCount}", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                                }
                                IconButton(onClick = { showDeleteDialog = candidate }) { Icon(Icons.Filled.Delete, "Hapus", tint = StatusError, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Candidate Sheet
    if (showCreateSheet) {
        CandidateFormSheet(onSave = { n, d, v, m, no, uri -> viewModel.createCandidate(electionId, n, d, v, m, no, uri); showCreateSheet = false }, onDismiss = { showCreateSheet = false })
    }

    // Delete Dialog
    showDeleteDialog?.let { candidate ->
        AlertDialog(onDismissRequest = { showDeleteDialog = null }, title = { Text("Hapus Kandidat?") }, text = { Text("Yakin hapus \"${candidate.name}\"?") }, confirmButton = { TextButton(onClick = { viewModel.deleteCandidate(candidate.id); showDeleteDialog = null }) { Text("Hapus", color = StatusError) } }, dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Batal") } })
    }

    state.error?.let { ErrorDialog(message = it, onDismiss = { viewModel.clearMessages() }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidateFormSheet(onSave: (String, String, String, String, Int, Uri?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var visi by remember { mutableStateOf("") }
    var misi by remember { mutableStateOf("") }
    var nomor by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
            Text("Tambah Kandidat", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = DeepBlue)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Kandidat") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi Singkat") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = visi, onValueChange = { visi = it }, label = { Text("Visi") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = misi, onValueChange = { misi = it }, label = { Text("Misi") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = nomor, onValueChange = { nomor = it }, label = { Text("Nomor Urut") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Image, null); Spacer(Modifier.width(8.dp)); Text(if (photo != null) "Foto dipilih" else "Pilih Foto Kandidat") }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onSave(name, desc, visi, misi, nomor.toIntOrNull() ?: 1, photo) }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen), shape = RoundedCornerShape(12.dp)) { Text("Simpan Kandidat", style = MaterialTheme.typography.labelLarge) }
        }
    }
}
