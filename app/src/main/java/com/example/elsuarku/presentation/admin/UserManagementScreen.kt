package com.example.elsuarku.presentation.admin

import android.widget.Toast
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
import com.example.elsuarku.data.model.User
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.data.model.UserStatus
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.presentation.components.SearchBar
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var showRoleDialog by remember { mutableStateOf<User?>(null) }
    var showStatusDialog by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Filter users by search query (case-insensitive match on name or email)
    val filteredUsers = remember(state.users, searchQuery) {
        if (searchQuery.isBlank()) state.users
        else state.users.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Pengguna", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading && state.users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(message = "Memuat data pengguna...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBadge("Total", state.users.size, DeepBlueLight, Icons.Filled.People, Modifier.weight(1f))
                    StatBadge("Admin", state.users.count { it.role == UserRole.ADMIN }, EmeraldGreen, Icons.Filled.AdminPanelSettings, Modifier.weight(1f))
                    StatBadge("Monitor", state.users.count { it.role == UserRole.MONITOR }, Gold, Icons.Filled.Visibility, Modifier.weight(1f))
                    StatBadge("Pemilih", state.users.count { it.role == UserRole.PEMILIH }, DeepBlue.copy(alpha = 0.6f), Icons.Filled.HowToVote, Modifier.weight(1f))
                }
            }

            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Cari pengguna...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    "Daftar Pengguna",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBlueDark
                )
            }

            if (state.users.isEmpty() && !state.isLoading) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.People, null, tint = DeepBlueLight, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Tidak ada data pengguna", color = DeepBlueLight)
                        }
                    }
                }
            } else {
                items(filteredUsers) { user ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Person, null,
                                tint = when (user.role) {
                                    UserRole.ADMIN -> EmeraldGreen
                                    UserRole.MONITOR -> Gold
                                    else -> DeepBlueLight
                                },
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user.name.ifBlank { "Tanpa Nama" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = DeepBlueDark)
                                Text("${user.email} | ${user.role.name}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = when (user.status) {
                                        UserStatus.ACTIVE -> EmeraldGreen.copy(alpha = 0.1f)
                                        UserStatus.SUSPENDED -> StatusWarning.copy(alpha = 0.1f)
                                        else -> StatusError.copy(alpha = 0.1f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        user.status.name,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (user.status) {
                                            UserStatus.ACTIVE -> EmeraldGreen
                                            UserStatus.SUSPENDED -> StatusWarning
                                            else -> StatusError
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { showRoleDialog = user }) {
                                Icon(Icons.Filled.Edit, "Ubah Role", tint = DeepBlue, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { showStatusDialog = user }) {
                                Icon(
                                    if (user.status == UserStatus.ACTIVE) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                    "Status",
                                    tint = if (user.status == UserStatus.ACTIVE) StatusError else EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showRoleDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showRoleDialog = null },
            title = { Text("Ubah Role: ${user.name}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    UserRole.entries.forEach { role ->
                        TextButton(onClick = { viewModel.updateUserRole(user.uid, role); showRoleDialog = null }) {
                            Text("${role.name} — ${roleDescription(role)}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRoleDialog = null }) { Text("Batal") } }
        )
    }

    showStatusDialog?.let { user ->
        val options = when (user.status) {
            UserStatus.ACTIVE -> listOf(
                UserStatus.SUSPENDED to "Suspend",
                UserStatus.BANNED to "Ban Permanen"
            )
            UserStatus.SUSPENDED -> listOf(
                UserStatus.ACTIVE to "Aktifkan",
                UserStatus.BANNED to "Ban Permanen"
            )
            UserStatus.BANNED -> listOf(
                UserStatus.ACTIVE to "Aktifkan Kembali"
            )
        }
        AlertDialog(
            onDismissRequest = { showStatusDialog = null },
            title = { Text("Ubah Status: ${user.name}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Status saat ini: ${user.status.name}")
                    Spacer(Modifier.height(8.dp))
                    options.forEach { (status, label) ->
                        val color = when (status) {
                            UserStatus.ACTIVE -> EmeraldGreen
                            UserStatus.SUSPENDED -> StatusWarning
                            UserStatus.BANNED -> StatusError
                        }
                        TextButton(
                            onClick = {
                                viewModel.updateUserStatus(user.uid, status)
                                showStatusDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, color = color, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showStatusDialog = null }) { Text("Batal") } }
        )
    }

    state.error?.let {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Error") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { viewModel.clearMessages() }) { Text("OK") } }
        )
    }
}

private fun roleDescription(role: UserRole) = when (role) {
    UserRole.ADMIN -> "Penyelenggara"
    UserRole.MONITOR -> "Pengawas"
    UserRole.PEMILIH -> "Pemilih"
}

@Composable
private fun StatBadge(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 12.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text("$value", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}
