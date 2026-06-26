package com.example.elsuarku.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.elsuarku.ui.theme.*

/**
 * Android 13+ notification permission gate.
 *
 * Wraps content and shows a permission prompt if notification permission
 * has not been granted. On Android 12 and below, content renders directly
 * (notification permission is not runtime-gated).
 *
 * @param title Permission rationale title
 * @param message Permission rationale message
 * @param content Composable rendered when permission is granted (or pre-13)
 */
@Composable
fun NotificationPermissionGate(
    title: String = "Aktifkan Notifikasi",
    message: String = "Dapatkan pemberitahuan saat pemilihan dimulai, pengingat sebelum ditutup, dan hasil akhir.",
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        content()
        return
    }

    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    if (permissionGranted) {
        content()
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DeepBlueDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = ColorWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnDeepBlue.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Aktifkan Notifikasi",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBlueDark
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = { permissionGranted = true }) {
                    Text("Nanti saja", color = OnDeepBlue.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private val ColorWhite = androidx.compose.ui.graphics.Color.White
