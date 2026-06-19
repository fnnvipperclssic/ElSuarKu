package com.example.elsuarku.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.auth.AuthViewModel
import com.example.elsuarku.security.BiometricPromptManager
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricPromptManager(context) }
    var biometricEnabled by remember { mutableStateOf(sessionManager.isBiometricEnabled()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", color = OnDeepBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftWhite)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ---- Security Section ----
            item {
                SectionHeader("Keamanan")
            }

            item {
                SettingsCard {
                    SettingsToggle(
                        icon = Icons.Filled.Fingerprint,
                        title = "Verifikasi Biometrik",
                        subtitle = "Gunakan sidik jari atau face ID untuk voting",
                        checked = biometricEnabled,
                        enabled = biometricManager.canAuthenticate().isAvailable,
                        onCheckedChange = { enabled ->
                            biometricEnabled = enabled
                            sessionManager.setBiometricEnabled(enabled)
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsInfo(
                        icon = Icons.Filled.Security,
                        title = "Enkripsi Data",
                        subtitle = "AES-256-GCM — Data suara Anda dienkripsi end-to-end",
                        value = "Aktif"
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsInfo(
                        icon = Icons.Filled.Shield,
                        title = "Perlindungan Layar",
                        subtitle = "Screenshot dan screen recording diblokir saat voting",
                        value = "Aktif"
                    )
                }
            }

            // ---- Account Section ----
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Akun")
            }

            item {
                val userName = sessionManager.getUserName()
                val userRole = sessionManager.getUserRole()?.name ?: "PEMILIH"
                SettingsCard {
                    SettingsInfo(
                        icon = Icons.Filled.Person,
                        title = userName.ifBlank { "Pengguna" },
                        subtitle = "Role: ${userRole.lowercase().replaceFirstChar { it.uppercase() }}",
                        value = ""
                    )
                }
            }

            // ---- About Section ----
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Tentang")
            }

            item {
                SettingsCard {
                    SettingsInfo(
                        icon = Icons.Filled.Info,
                        title = "Versi Aplikasi",
                        subtitle = "ElSuarKu — Cloud-Based Secure E-Voting Platform",
                        value = "1.0.0"
                    )
                }
            }

            // ---- Logout ----
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Logout, null, tint = StatusError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar dari Akun", color = StatusError)
                }
            }
        }
    }

    // Logout confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Keluar") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun? Sesi Anda akan diakhiri.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Keluar", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = DeepBlue,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DeepBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = DeepBlueDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsInfo(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = DeepBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = DeepBlueDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
        }
        if (value.isNotBlank()) {
            Surface(color = EmeraldGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                Text(
                    value,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
