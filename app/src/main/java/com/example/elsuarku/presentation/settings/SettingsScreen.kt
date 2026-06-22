package com.example.elsuarku.presentation.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.auth.AuthViewModel
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.security.BiometricPromptManager
import com.example.elsuarku.security.BiometricResult
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
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricManager = remember { BiometricPromptManager() }
    var biometricEnabled by remember { mutableStateOf(sessionManager.isBiometricEnabled()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Security Section ──
            item {
                SectionHeader("Keamanan")
            }

            item {
                val biometricResult = remember {
                    activity?.let { biometricManager.canAuthenticate(it) } ?: BiometricResult.Unknown
                }
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    SettingsToggle(
                        icon = Icons.Filled.Fingerprint,
                        title = "Verifikasi Biometrik",
                        subtitle = "Gunakan sidik jari atau face ID untuk voting",
                        checked = biometricEnabled,
                        enabled = biometricResult.isAvailable,
                        onCheckedChange = { enabled ->
                            biometricEnabled = enabled
                            sessionManager.setBiometricEnabled(enabled)
                        }
                    )
                    if (!biometricResult.isAvailable) {
                        val (message, actionLabel) = when (biometricResult) {
                            is BiometricResult.NotEnrolled -> "Belum ada sidik jari/wajah terdaftar di perangkat ini." to "Daftarkan"
                            is BiometricResult.NoHardware -> "Perangkat ini tidak memiliki sensor biometrik." to null
                            is BiometricResult.HardwareUnavailable -> "Sensor biometrik sedang tidak tersedia." to null
                            is BiometricResult.SecurityUpdateRequired -> "Diperlukan pembaruan keamanan sistem." to "Pengaturan"
                            else -> "Biometrik tidak tersedia." to null
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp, end = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, null, tint = StatusWarning, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                message,
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusWarning,
                                modifier = Modifier.weight(1f)
                            )
                            if (actionLabel != null) {
                                TextButton(onClick = {
                                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                }) {
                                    Text(actionLabel, style = MaterialTheme.typography.labelSmall, color = Gold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    SettingsInfo(
                        icon = Icons.Filled.Security,
                        title = "Enkripsi Data",
                        subtitle = "AES-256-GCM — Data suara Anda dienkripsi end-to-end",
                        value = "Aktif"
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    SettingsInfo(
                        icon = Icons.Filled.Shield,
                        title = "Perlindungan Layar",
                        subtitle = "Screenshot dan screen recording diblokir saat voting",
                        value = "Aktif"
                    )
                }
            }

            // ── Account Section ──
            item { Spacer(Modifier.height(4.dp)) }
            item { SectionHeader("Akun") }

            item {
                val userName = sessionManager.getUserName()
                val userRole = sessionManager.getUserRole()?.name ?: "PEMILIH"
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    SettingsInfo(
                        icon = Icons.Filled.Person,
                        title = userName.ifBlank { "Pengguna" },
                        subtitle = "Role: ${userRole.lowercase().replaceFirstChar { it.uppercase() }}",
                        value = ""
                    )
                }
            }

            // ── About Section ──
            item { Spacer(Modifier.height(4.dp)) }
            item { SectionHeader("Tentang") }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    SettingsInfo(
                        icon = Icons.Filled.Info,
                        title = "Versi Aplikasi",
                        subtitle = "ElSuarKu — Cloud-Based Secure E-Voting Platform",
                        value = "1.0.0"
                    )
                }
            }

            // ── Logout ──
            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = StatusError, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Keluar dari Akun", color = StatusError, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Keluar", fontWeight = FontWeight.SemiBold) },
            text = { Text("Apakah Anda yakin ingin keluar dari akun? Sesi Anda akan diakhiri.") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("Keluar", color = StatusError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
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
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DeepBlue.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = DeepBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = DeepBlueDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = Gold.copy(alpha = 0.3f))
        )
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DeepBlue.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = DeepBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = DeepBlueDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DeepBlueLight)
        }
        if (value.isNotBlank()) {
            Surface(
                color = EmeraldGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    value,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldGreen
                )
            }
        }
    }
}
