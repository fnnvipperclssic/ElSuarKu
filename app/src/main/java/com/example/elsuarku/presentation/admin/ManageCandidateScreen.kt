package com.example.elsuarku.presentation.admin

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCandidateScreen(
    electionId: String,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf<Candidate?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Candidate?>(null) }
    val context = LocalContext.current

    LaunchedEffect(electionId) { viewModel.loadCandidates(electionId) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.selectedElection?.title ?: "Kelola Kandidat",
                        color = OnDeepBlue,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(Icons.Filled.PersonAdd, "Tambah Kandidat", tint = Gold, modifier = Modifier.size(26.dp))
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.error != null && state.candidates.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Error, null, tint = StatusError, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Gagal memuat kandidat", style = MaterialTheme.typography.titleMedium, color = StatusError, fontWeight = FontWeight.SemiBold)
                                Text(state.error ?: "Error tidak diketahui", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(onClick = { viewModel.loadCandidates(electionId) }) { Text("Coba Lagi") }
                            }
                        }
                    }
                } else if (state.candidates.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.People, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Belum ada kandidat", style = MaterialTheme.typography.titleMedium, color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                                Text("Klik + untuk menambahkan kandidat", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                            }
                        }
                    }
                } else {
                    items(state.candidates, key = { it.id }) { candidate ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (candidate.nomorUrut <= 2) Gold.copy(alpha = 0.15f) else DeepBlueLight.copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${candidate.nomorUrut}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (candidate.nomorUrut <= 2) Gold else DeepBlueLight
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(candidate.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = DeepBlueDark)
                                    if (candidate.visi.isNotBlank()) Text(candidate.visi, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight, maxLines = 1)
                                    Text("Suara: ${candidate.voteCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = EmeraldGreen)
                                }
                                IconButton(onClick = { showEditSheet = candidate }) {
                                    Icon(Icons.Filled.Edit, "Edit", tint = DeepBlue, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { showDeleteDialog = candidate }) {
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
        CandidateFormSheet(
            title = "Tambah Kandidat",
            candidate = null,
            onSave = { n, d, v, m, no, uri ->
                viewModel.createCandidate(electionId, n, d, v, m, no, uri)
                showCreateSheet = false
            },
            onDismiss = { showCreateSheet = false }
        )
    }

    showEditSheet?.let { candidate ->
        CandidateFormSheet(
            title = "Edit Kandidat",
            candidate = candidate,
            onSave = { n, d, v, m, no, uri ->
                val updated = candidate.copy(name = n, description = d, visi = v, misi = m, nomorUrut = no)
                if (uri != null) {
                    // Photo changed — update with new photo
                    viewModel.updateCandidatePhoto(candidate.id, uri)
                }
                viewModel.updateCandidateInfo(updated)
                showEditSheet = null
            },
            onDismiss = { showEditSheet = null }
        )
    }

    showDeleteDialog?.let { candidate ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Hapus Kandidat?", fontWeight = FontWeight.SemiBold) },
            text = { Text("Yakin hapus \"${candidate.name}\"?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCandidate(candidate.id); showDeleteDialog = null }) {
                    Text("Hapus", color = StatusError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Batal") } }
        )
    }

    state.error?.let {
        ErrorDialog(message = it, onDismiss = { viewModel.clearMessages() }, onRetry = { viewModel.loadCandidates(electionId) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidateFormSheet(
    title: String,
    candidate: Candidate?,
    onSave: (String, String, String, String, Int, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(candidate?.name ?: "") }
    var desc by remember { mutableStateOf(candidate?.description ?: "") }
    var visi by remember { mutableStateOf(candidate?.visi ?: "") }
    var misi by remember { mutableStateOf(candidate?.misi ?: "") }
    var nomor by remember { mutableStateOf(if (candidate != null && candidate.nomorUrut > 0) candidate.nomorUrut.toString() else "") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var nomorError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }
    val context = LocalContext.current

    // Decode existing photo for preview
    val existingBitmap = remember(candidate?.photoBase64) {
        candidate?.photoBase64?.takeIf { it.isNotBlank() }?.let {
            try {
                val bytes = android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = DeepBlue)
            Spacer(Modifier.height(20.dp))

            // Photo preview section
            if (existingBitmap != null || photo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewBitmap = if (photo != null) {
                            // Preview newly selected photo from URI
                            try {
                                val inputStream = context.contentResolver.openInputStream(photo!!)
                                val bytes = inputStream?.readBytes()
                                inputStream?.close()
                                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                            } catch (_: Exception) { null }
                        } else existingBitmap
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Preview foto",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Person, null, tint = DeepBlueLight, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Kandidat") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi Singkat") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = visi, onValueChange = { visi = it }, label = { Text("Visi") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = misi, onValueChange = { misi = it }, label = { Text("Misi") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = nomor, onValueChange = { nomor = it; nomorError = null },
                label = { Text("Nomor Urut") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = nomorError != null,
                supportingText = nomorError?.let { { Text(it, color = StatusError) } }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { picker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (photo != null) "Foto dipilih" else if (candidate?.photoBase64?.isNotBlank() == true) "Ganti Foto" else "Pilih Foto Kandidat")
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val num = nomor.toIntOrNull()
                    if (num == null || num <= 0) {
                        nomorError = "Nomor urut harus angka positif (minimal 1)"
                        return@Button
                    }
                    nomorError = null
                    onSave(name, desc, visi, misi, num, photo)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (candidate == null) "Simpan Kandidat" else "Simpan Perubahan", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
