package com.example.elsuarku.presentation.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.security.PinManager
import com.example.elsuarku.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPinScreen(
    pinManager: PinManager,
    onPinSetupSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1 = enter, 2 = confirm
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    val currentPin = if (step == 1) pin1 else pin2
    val isPinComplete = currentPin.length == 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Setup PIN", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlueDark
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepBlueDarker, DeepBlueDark, DeepBlue)
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Gold
            )

            Spacer(Modifier.height(24.dp))

            // Title
            Text(
                text = if (step == 1) "Masukkan PIN" else "Konfirmasi PIN",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = OnDeepBlue
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = if (step == 1) "Masukkan PIN 4 digit untuk login cepat" else "Masukkan ulang PIN Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = OnDeepBlue.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // PIN Display (4 dots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    val isFilled = index < currentPin.length
                    val dotColor by animateColorAsState(
                        targetValue = if (isFilled) Gold else DeepBlueDark.copy(alpha = 0.6f),
                        animationSpec = tween(200),
                        label = "dotColor$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFilled) Gold.copy(alpha = 0.2f) else DeepBlueDark.copy(alpha = 0.4f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        if (isFilled) Gold else OnDeepBlue.copy(alpha = 0.2f),
                                        if (isFilled) GoldLight else OnDeepBlue.copy(alpha = 0.1f)
                                    )
                                )
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isFilled) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                brush = Brush.horizontalGradient(listOf(Gold, GoldLight)),
                                                shape = RoundedCornerShape(7.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = StatusError,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Numeric Keypad
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Row 1: 1 2 3
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("1", "2", "3").forEach { digit ->
                        PinKeyButton(digit = digit, enabled = !isPinComplete) {
                            appendDigit(digit, currentPin) { newPin ->
                                if (step == 1) pin1 = newPin else pin2 = newPin
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Row 2: 4 5 6
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("4", "5", "6").forEach { digit ->
                        PinKeyButton(digit = digit, enabled = !isPinComplete) {
                            appendDigit(digit, currentPin) { newPin ->
                                if (step == 1) pin1 = newPin else pin2 = newPin
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Row 3: 7 8 9
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("7", "8", "9").forEach { digit ->
                        PinKeyButton(digit = digit, enabled = !isPinComplete) {
                            appendDigit(digit, currentPin) { newPin ->
                                if (step == 1) pin1 = newPin else pin2 = newPin
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Row 4: Clear, 0, Delete
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Clear button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (step == 1) pin1 = "" else pin2 = ""
                        },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepBlueDark.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "C",
                            fontWeight = FontWeight.Bold,
                            color = OnDeepBlue.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // 0
                    PinKeyButton(digit = "0", enabled = !isPinComplete) {
                        appendDigit("0", currentPin) { newPin ->
                            if (step == 1) pin1 = newPin else pin2 = newPin
                        }
                    }

                    // Delete button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (step == 1) {
                                if (pin1.isNotEmpty()) pin1 = pin1.dropLast(1)
                            } else {
                                if (pin2.isNotEmpty()) pin2 = pin2.dropLast(1)
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepBlueDark.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Hapus",
                            tint = OnDeepBlue.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Auto-advance & Verification ──

    // Step 1 complete → go to step 2
    LaunchedEffect(pin1) {
        if (step == 1 && pin1.length == 4) {
            step = 2
        }
    }

    // Step 2 complete → verify
    LaunchedEffect(pin2) {
        if (step == 2 && pin2.length == 4) {
            if (pin1 == pin2) {
                if (pinManager.setPin(pin1)) {
                    showSuccess = true
                } else {
                    errorMessage = "Gagal menyimpan PIN. Pastikan PIN 4 digit angka."
                    pin2 = ""
                }
            } else {
                errorMessage = "PIN tidak cocok. Silakan coba lagi."
                pin2 = ""
            }
        }
    }

    // ── Success Dialog ──
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "PIN Berhasil Dibuat!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "PIN Anda telah dibuat dan disimpan dengan aman. " +
                            "Gunakan PIN ini untuk login cepat.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccess = false
                        onPinSetupSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Selesai", color = DeepBlueDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DeepBlueDark,
            titleContentColor = OnDeepBlue,
            textContentColor = OnDeepBlue.copy(alpha = 0.7f),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Append a digit to the PIN, enforcing 4-digit max.
 */
private fun appendDigit(digit: String, current: String, onUpdate: (String) -> Unit) {
    if (current.length < 4) {
        onUpdate(current + digit)
    }
}

/**
 * Numeric keypad button for PIN entry.
 */
@Composable
private fun PinKeyButton(
    digit: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepBlueDark.copy(alpha = 0.5f),
            disabledContainerColor = DeepBlueDark.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) OnDeepBlue else OnDeepBlue.copy(alpha = 0.3f)
        )
    }
}
