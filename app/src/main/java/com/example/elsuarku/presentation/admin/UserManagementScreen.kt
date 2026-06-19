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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.model.User
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.data.model.UserStatus
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var showRoleDialog by remember { mutableStateOf<User?>(null) }
    var showStatusDialog by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) { viewModel.loadAllData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Pengguna", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBadge("Total User", state.users.size, DeepBlueLight, Modifier.weight(1f))
                    StatBadge("Admin", state.users.count { it.role == UserRole.ADMIN }, EmeraldGreen, Modifier.weight(1f))
                    StatBadge("Monitor", state.users.count { it.role == UserRole.MONITOR }, Gold, Modifier.weight(1f))
                    StatBadge("Pemilih", state.users.count { it.role == UserRole.PEMILIH }, DeepBlue.copy(alpha = 0.6f), Modifier.weight(1f))
                }
            }
            item { Text("Daftar Pengguna", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlueDark) }
            if (state.users.isEmpty()) {
                item { Text("Tidak ada data pengguna", color = DeepBlueLight, modifier = Modifier.padding(16.dp)) }
            } else {
                items(state.users) { user ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = when (user.role) { UserRole.ADMIN -> EmeraldGreen; UserRole.MONITOR -> Gold; else -> DeepBlueLight }, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user.name.ifBlank { "Tanpa Nama" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DeepBlueDark)
                                Text("${user.email} | ${user.role.name}", style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
                                Surface(color = when (user.status) { UserStatus.ACTIVE -> EmeraldGreen.copy(alpha = 0.1f); UserStatus.SUSPENDED -> StatusWarning.copy(alpha = 0.1f); else -> StatusError.copy(alpha = 0.1f) }, shape = RoundedCornerShape(4.dp)) {
                                    Text(user.status.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = when (user.status) { UserStatus.ACTIVE -> EmeraldGreen; UserStatus.SUSPENDED -> StatusWarning; else -> StatusError })
                                }
                            }
                            IconButton(onClick = { showRoleDialog = user }) { Icon(Icons.Filled.Edit, "Ubah Role", tint = DeepBlue) }
                            IconButton(onClick = { showStatusDialog = user }) { Icon(if (user.status == UserStatus.ACTIVE) Icons.Filled.Block else Icons.Filled.CheckCircle, "Status", tint = if (user.status == UserStatus.ACTIVE) StatusError else EmeraldGreen) }
                        }
                    }
                }
            }
        }
    }

    // Role Dialog
    showRoleDialog?.let { user ->
        AlertDialog(onDismissRequest = { showRoleDialog = null }, title = { Text("Ubah Role: ${user.name}") }, text = {
            Column { UserRole.entries.forEach { role -> TextButton(onClick = { viewModel.updateUserRole(user.uid, role); showRoleDialog = null }) { Text("${role.name} — ${roleDescription(role)}") } } }
        }, confirmButton = { TextButton(onClick = { showRoleDialog = null }) { Text("Batal") } })
    }

    // Status Dialog
    showStatusDialog?.let { user ->
        AlertDialog(onDismissRequest = { showStatusDialog = null }, title = { Text("Ubah Status: ${user.name}") }, text = { Text("Status saat ini: ${user.status.name}") }, confirmButton = {
            if (user.status == UserStatus.ACTIVE) TextButton(onClick = { viewModel.updateUserStatus(user.uid, UserStatus.SUSPENDED); showStatusDialog = null }) { Text("Suspend", color = StatusError) }
            else TextButton(onClick = { viewModel.updateUserStatus(user.uid, UserStatus.ACTIVE); showStatusDialog = null }) { Text("Aktifkan", color = EmeraldGreen) }
        }, dismissButton = { TextButton(onClick = { showStatusDialog = null }) { Text("Batal") } })
    }

    state.error?.let { ErrorDialog(it, onDismiss = { viewModel.clearMessages() }) }
    state.successMessage?.let { LaunchedEffect(it) { viewModel.clearMessages() } }
}

private fun roleDescription(role: UserRole) = when (role) { UserRole.ADMIN -> "Penyelenggara"; UserRole.MONITOR -> "Pengawas"; UserRole.PEMILIH -> "Pemilih" }

@Composable
private fun StatBadge(label: String, value: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Error") }, text = { Text(message) }, confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } })
}
