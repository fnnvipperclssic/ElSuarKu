package com.example.elsuarku.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.auth.AuthViewModel
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: DashboardViewModel,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val userName = sessionManager.getUserName()
    val userRole = sessionManager.getUserRole()?.name ?: "PEMILIH"
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profil Saya", color = OnDeepBlue, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = OnDeepBlue) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue))
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar
            Surface(modifier = Modifier.size(96.dp), shape = RoundedCornerShape(48.dp), color = DeepBlueLight.copy(alpha = 0.3f)) { Box(contentAlignment = Alignment.Center) { Text(userName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = DeepBlue) } }
            Spacer(Modifier.height(16.dp))
            Text(userName.ifBlank { "Pengguna" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DeepBlueDark)
            Surface(color = when (userRole) { "ADMIN" -> EmeraldGreen.copy(alpha = 0.1f); "MONITOR" -> Gold.copy(alpha = 0.1f); else -> DeepBlueLight.copy(alpha = 0.1f) }, shape = RoundedCornerShape(8.dp)) { Text(userRole, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = when (userRole) { "ADMIN" -> EmeraldGreen; "MONITOR" -> Gold; else -> DeepBlueLight }) }

            Spacer(Modifier.height(32.dp))

            // Info Cards
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                Column(Modifier.padding(16.dp)) {
                    ProfileRow(Icons.Filled.Person, "Nama", userName.ifBlank { "Belum diatur" })
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    ProfileRow(Icons.Filled.Badge, "Role", userRole)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    ProfileRow(Icons.Filled.Security, "Keamanan", "Akun Terverifikasi")
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    ProfileRow(Icons.Filled.HowToVote, "Voting", "${state.votedElectionIds.size} pemilihan diikuti")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Voting Stats
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = EmeraldGreenSurface)) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Anda telah berpartisipasi dalam ${state.votedElectionIds.size} pemilihan", style = MaterialTheme.typography.bodyMedium, color = EmeraldGreenDark)
                }
            }

            Spacer(Modifier.height(32.dp))

            OutlinedButton(onClick = { showLogoutDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)) { Icon(Icons.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Keluar dari Akun") }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(onDismissRequest = { showLogoutDialog = false }, title = { Text("Konfirmasi Keluar") }, text = { Text("Apakah Anda yakin ingin keluar?") }, confirmButton = { TextButton(onClick = { showLogoutDialog = false; onLogout() }) { Text("Keluar", color = StatusError) } }, dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") } })
    }
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = DeepBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall, color = DeepBlueLight); Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = DeepBlueDark) }
    }
}
