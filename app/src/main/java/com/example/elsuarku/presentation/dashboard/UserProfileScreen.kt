package com.example.elsuarku.presentation.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.elsuarku.data.repository.ImageStorage
import com.example.elsuarku.presentation.auth.AuthViewModel
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: DashboardViewModel,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    imageStorage: ImageStorage,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val userName = sessionManager.getUserName()
    val userId = sessionManager.getUserId() ?: ""
    val userRole = sessionManager.getUserRole()?.name ?: "PEMILIH"
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            val result = viewModel.loadUserPhoto(userId)
            if (result != null) photoBase64 = result
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingPhoto = true
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = imageStorage.uriToBase64(uri)
                if (base64 != null) {
                    if (!imageStorage.isWithinSizeLimit(base64)) {
                        withContext(Dispatchers.Main) {
                            photoError = "Foto terlalu besar (maks ~900KB). Pilih foto yang lebih kecil."
                            isUploadingPhoto = false
                        }
                    } else {
                        val success = viewModel.updateProfilePhoto(userId, base64)
                        withContext(Dispatchers.Main) {
                            if (success) photoBase64 = base64
                            else photoError = "Gagal menyimpan foto ke server. Periksa koneksi dan coba lagi."
                            isUploadingPhoto = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        photoError = "Gagal memproses gambar. Coba foto lain."
                        isUploadingPhoto = false
                    }
                }
            }
        }
    }

    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) photoPickerLauncher.launch("image/*")
        else showPermissionDeniedDialog = true
    }

    fun pickPhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch("image/*")
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                photoPickerLauncher.launch("image/*")
            } else {
                readPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", color = OnDeepBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftWhite)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Avatar with photo picker — offload bitmap decode to background thread
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                val bitmap by produceState<android.graphics.Bitmap?>(null, photoBase64) {
                    value = photoBase64?.let { b64 ->
                        try {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                        } catch (_: Exception) { null }
                    }
                }

                val photoBitmap = bitmap  // local val for smart-cast (delegated props can't be smart-cast)
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, Gold.copy(alpha = 0.4f), CircleShape)
                            .clickable { pickPhoto() },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(DeepBlueLight.copy(alpha = 0.5f), DeepBlue.copy(alpha = 0.3f))
                                )
                            )
                            .border(3.dp, DeepBlueLight.copy(alpha = 0.3f), CircleShape)
                            .clickable { pickPhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingPhoto) {
                            LoadingIndicator()
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    userName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = DeepBlue
                                )
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = "Ubah foto",
                                    tint = DeepBlue.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Edit badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp),
                    shape = CircleShape,
                    color = Gold,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = DeepBlueDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Ketuk untuk ubah foto",
                style = MaterialTheme.typography.labelSmall,
                color = DeepBlueLight.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                userName.ifBlank { "Pengguna" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = DeepBlueDark
            )

            Spacer(Modifier.height(6.dp))

            // Role badge
            Surface(
                color = when (userRole) {
                    "ADMIN" -> EmeraldGreen.copy(alpha = 0.1f)
                    "MONITOR" -> Gold.copy(alpha = 0.1f)
                    else -> DeepBlueLight.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    userRole,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = when (userRole) {
                        "ADMIN" -> EmeraldGreen
                        "MONITOR" -> Gold
                        else -> DeepBlueLight
                    }
                )
            }

            Spacer(Modifier.height(28.dp))

            // Info card
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                ProfileRow(Icons.Filled.Person, "Nama", userName.ifBlank { "Belum diatur" })
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    color = DeepBlueLight.copy(alpha = 0.1f)
                )
                ProfileRow(Icons.Filled.Email, "Email", sessionManager.getUserEmail().ifBlank { "Tidak tersedia" })
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    color = DeepBlueLight.copy(alpha = 0.1f)
                )
                ProfileRow(Icons.Filled.Badge, "Role", userRole)
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    color = DeepBlueLight.copy(alpha = 0.1f)
                )
                ProfileRow(Icons.Filled.Security, "Keamanan", "Akun Terverifikasi")
                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    color = DeepBlueLight.copy(alpha = 0.1f)
                )
                ProfileRow(Icons.Filled.HowToVote, "Voting", "${state.votedElectionIds.size} pemilihan diikuti")
            }

            Spacer(Modifier.height(16.dp))

            // Photo tip
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Gold.copy(alpha = 0.06f)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Foto profil akan ditampilkan di konfirmasi voting.\nUkuran maksimal ~900KB (dikompres otomatis).",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepBlueDark.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(listOf(StatusError.copy(alpha = 0.3f), StatusError.copy(alpha = 0.1f)))
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Keluar dari Akun", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            icon = { Icon(Icons.Filled.Warning, null, tint = StatusWarning) },
            title = { Text("Izin Diperlukan") },
            text = {
                Text(
                    "Untuk mengubah foto profil, ElSuarKu memerlukan izin mengakses penyimpanan.\n\n" +
                            "Buka Pengaturan > Aplikasi > ElSuarKu > Izin > lalu aktifkan \"Penyimpanan\" atau \"Foto & Media\"."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDeniedDialog = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }) { Text("Buka Pengaturan") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) { Text("Nanti") }
            }
        )
    }

    photoError?.let { error ->
        ErrorDialog(title = "Upload Gagal", message = error, onDismiss = { photoError = null })
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Keluar", fontWeight = FontWeight.SemiBold) },
            text = { Text("Apakah Anda yakin ingin keluar?") },
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
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DeepBlue.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = DeepBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = DeepBlueLight)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = DeepBlueDark
            )
        }
    }
}
