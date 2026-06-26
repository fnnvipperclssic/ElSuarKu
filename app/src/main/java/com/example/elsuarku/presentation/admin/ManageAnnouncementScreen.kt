package com.example.elsuarku.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.Announcement
import com.example.elsuarku.data.model.AnnouncementPriority
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*
import com.example.elsuarku.utils.toFormattedDateTime
import com.example.elsuarku.utils.showToast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAnnouncementScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadAnnouncements() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Pengumuman", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadAnnouncements()
                        context.showToast("Pengumuman dimuat ulang")
                    }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Gold,
                contentColor = DeepBlueDark
            ) {
                Icon(Icons.Filled.Add, "Buat Pengumuman")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftWhite)
                .padding(padding)
        ) {
            // Success/error messages
            state.successMessage?.let { msg ->
                Surface(color = EmeraldGreen.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = EmeraldGreen,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                LaunchedEffect(msg) { viewModel.clearMessages() }
            }
            state.error?.let { err ->
                Surface(color = StatusError.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        err,
                        modifier = Modifier.padding(12.dp),
                        color = StatusError,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                LaunchedEffect(err) { viewModel.clearMessages() }
            }

            if (state.isAnnouncementLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(message = "Memuat pengumuman...")
                }
            } else if (state.announcements.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Campaign, null, tint = DeepBlueLight, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Belum ada pengumuman", style = MaterialTheme.typography.titleMedium, color = DeepBlueDark, fontWeight = FontWeight.SemiBold)
                        Text("Tekan + untuk membuat pengumuman baru", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.announcements, key = { it.id }) { announcement ->
                        val priority = AnnouncementPriority.fromString(announcement.priority)
                        val accentColor = when (priority) {
                            AnnouncementPriority.HIGH -> StatusError
                            AnnouncementPriority.NORMAL -> StatusWarning
                            else -> DeepBlueLight
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Campaign,
                                        null,
                                        tint = if (announcement.isActive) accentColor else DeepBlueLight.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = accentColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            priority.name,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = accentColor
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (!announcement.isActive) {
                                        Surface(
                                            color = DeepBlueLight.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "NONAKTIF",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepBlueLight
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        announcement.createdAt.toFormattedDateTime(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepBlueLight.copy(alpha = 0.5f)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    announcement.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = DeepBlueDark
                                )
                                if (announcement.message.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        announcement.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DeepBlueDark.copy(alpha = 0.7f),
                                        maxLines = 3
                                    )
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Toggle Active
                                    TextButton(onClick = { viewModel.toggleAnnouncementActive(announcement) }) {
                                        Icon(
                                            if (announcement.isActive) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (announcement.isActive) StatusWarning else EmeraldGreen
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            if (announcement.isActive) "Nonaktifkan" else "Aktifkan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (announcement.isActive) StatusWarning else EmeraldGreen
                                        )
                                    }
                                    // Edit
                                    TextButton(onClick = { editingAnnouncement = announcement }) {
                                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp), tint = DeepBlue)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit", style = MaterialTheme.typography.labelSmall, color = DeepBlue)
                                    }
                                    // Delete
                                    TextButton(onClick = { deleteConfirmId = announcement.id }) {
                                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp), tint = StatusError)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Hapus", style = MaterialTheme.typography.labelSmall, color = StatusError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Create Dialog ──
    if (showCreateDialog) {
        AnnouncementFormDialog(
            title = "Buat Pengumuman",
            onDismiss = { showCreateDialog = false },
            onSave = { title, message, priority ->
                viewModel.createAnnouncement(title, message, priority)
                showCreateDialog = false
            }
        )
    }

    // ── Edit Dialog ──
    editingAnnouncement?.let { existing ->
        AnnouncementFormDialog(
            title = "Edit Pengumuman",
            initialTitle = existing.title,
            initialMessage = existing.message,
            initialPriority = existing.priority,
            onDismiss = { editingAnnouncement = null },
            onSave = { title, message, priority ->
                viewModel.updateAnnouncement(existing.copy(title = title, message = message, priority = priority))
                editingAnnouncement = null
            }
        )
    }

    // ── Delete Confirmation ──
    deleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("Hapus Pengumuman?", fontWeight = FontWeight.Bold) },
            text = { Text("Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAnnouncement(id)
                    deleteConfirmId = null
                }) {
                    Text("Hapus", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text("Batal", color = DeepBlueLight)
                }
            },
            containerColor = DeepBlueDark,
            titleContentColor = OnDeepBlue,
            textContentColor = OnDeepBlue.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementFormDialog(
    title: String,
    initialTitle: String = "",
    initialMessage: String = "",
    initialPriority: String = "NORMAL",
    onDismiss: () -> Unit,
    onSave: (title: String, message: String, priority: String) -> Unit
) {
    var inputTitle by remember { mutableStateOf(initialTitle) }
    var inputMessage by remember { mutableStateOf(initialMessage) }
    var selectedPriority by remember { mutableStateOf(initialPriority) }
    var expanded by remember { mutableStateOf(false) }
    val isFormValid = inputTitle.isNotBlank()
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = OnDeepBlue,
        unfocusedTextColor = OnDeepBlue.copy(alpha = 0.75f),
        focusedBorderColor = Gold,
        unfocusedBorderColor = OnDeepBlue.copy(alpha = 0.2f),
        focusedLabelColor = Gold,
        unfocusedLabelColor = OnDeepBlue.copy(alpha = 0.5f),
        cursorColor = Gold,
        focusedContainerColor = DeepBlueDark.copy(alpha = 0.4f),
        unfocusedContainerColor = DeepBlueDark.copy(alpha = 0.2f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = OnDeepBlue)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inputTitle,
                    onValueChange = { if (it.length <= 300) inputTitle = it },
                    label = { Text("Judul") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { if (it.length <= 2000) inputMessage = it },
                    label = { Text("Pesan") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(10.dp)
                )
                // Priority dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = AnnouncementPriority.fromString(selectedPriority).name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Prioritas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AnnouncementPriority.entries.forEach { prio ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val color = when (prio) {
                                            AnnouncementPriority.HIGH -> StatusError
                                            AnnouncementPriority.NORMAL -> StatusWarning
                                            else -> DeepBlueLight
                                        }
                                        Surface(
                                            color = color.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                prio.name,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = color
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedPriority = prio.firestoreValue
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(inputTitle.trim(), inputMessage.trim(), selectedPriority) },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan", color = DeepBlueDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = DeepBlueLight)
            }
        },
        containerColor = DeepBlueDark,
        titleContentColor = OnDeepBlue,
        textContentColor = OnDeepBlue.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp)
    )
}
