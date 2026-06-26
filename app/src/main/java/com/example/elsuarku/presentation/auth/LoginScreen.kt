package com.example.elsuarku.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.elsuarku.BuildConfig
import com.example.elsuarku.data.SeedUsers
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.security.BiometricPromptManager
import com.example.elsuarku.security.BiometricResult
import com.example.elsuarku.security.PinManager
import com.example.elsuarku.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    pinManager: PinManager,
    onNavigateToSetupPin: () -> Unit = {}
) {
    val loginState by authViewModel.loginState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var biometricStatus by remember { mutableStateOf<BiometricResult?>(null) }
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val coroutineScope = rememberCoroutineScope()

    // Client-side validation
    val isEmailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isFormValid = email.isNotBlank() && isEmailValid && password.isNotBlank()

    // Entrance animation
    val entryAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(400), label = "entry")
    val entryOffset by animateDpAsState(targetValue = 0.dp, animationSpec = tween(500, easing = FastOutSlowInEasing), label = "entry_offset")

    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.example.elsuarku.R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isGoogleSigningIn = true
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    authViewModel.signInWithGoogle(idToken)
                } ?: run {
                    showError = "Gagal mendapatkan kredensial Google."
                    isGoogleSigningIn = false
                }
            } catch (e: ApiException) {
                showError = "Google Sign-In gagal: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}"
                isGoogleSigningIn = false
            }
        } else {
            isGoogleSigningIn = false
        }
    }

    // Check biometric availability on first composition
    LaunchedEffect(Unit) {
        val bioManager = BiometricPromptManager()
        biometricStatus = bioManager.canAuthenticate(activity)
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is AuthViewModel.LoginUiState.Success -> {
                onLoginSuccess(state.role.name)
                authViewModel.resetLoginState()
            }
            is AuthViewModel.LoginUiState.Error -> {
                showError = state.message
                authViewModel.resetLoginState()
                isGoogleSigningIn = false
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepBlueDarker, DeepBlueDark, DeepBlue)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .alpha(entryAlpha)
                .offset(y = entryOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Brand Hero ──
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Gold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "ElSuarKu",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = OnDeepBlue,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Platform E-Voting Berbasis Cloud",
                style = MaterialTheme.typography.bodyLarge,
                color = OnDeepBlue.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Aman • Transparan • Cepat • Terpercaya",
                style = MaterialTheme.typography.bodySmall,
                color = Gold.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // ── Login Card ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isDark = true,
                cornerRadius = 20.dp
            ) {
                // Google Sign-In
                OutlinedButton(
                    onClick = {
                        isGoogleSigningIn = true
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isGoogleSigningIn && loginState !is AuthViewModel.LoginUiState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Gold, GoldLight))
                    )
                ) {
                    if (isGoogleSigningIn) {
                        LoadingIndicator()
                    } else {
                        Text(
                            "G",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Gold
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Masuk dengan Google",
                            color = OnDeepBlue.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(Modifier.weight(1f).height(1.dp)) {
                        drawLine(
                            OnDeepBlue.copy(alpha = 0.15f),
                            Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f
                        )
                    }
                    Text(
                        "  atau masuk dengan email  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnDeepBlue.copy(alpha = 0.4f)
                    )
                    Canvas(Modifier.weight(1f).height(1.dp)) {
                        drawLine(
                            OnDeepBlue.copy(alpha = 0.15f),
                            Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, "Email", tint = OnDeepBlue.copy(alpha = 0.6f))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = !isEmailValid,
                    supportingText = if (!isEmailValid) {
                        { Text("Format email tidak valid", color = StatusError) }
                    } else null,
                    colors = authTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, "Password", tint = OnDeepBlue.copy(alpha = 0.6f))
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                "Toggle password",
                                tint = OnDeepBlue.copy(alpha = 0.6f)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    colors = authTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(24.dp))

                // Login button
                Button(
                    onClick = { authViewModel.signInWithEmail(email, password) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = isFormValid && loginState !is AuthViewModel.LoginUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loginState is AuthViewModel.LoginUiState.Loading) {
                        LoadingIndicator()
                    } else {
                        Text(
                            "Masuk",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = DeepBlueDark
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Register link
                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Belum punya akun? Daftar di sini",
                        color = Gold.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // ── Biometric / PIN Quick Login Section ──
                // Always visible — biometric when hardware available, PIN/Setup always shown
                val showBioButton = biometricStatus?.isAvailable == true

                // Divider
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(Modifier.weight(1f).height(1.dp)) {
                        drawLine(
                            OnDeepBlue.copy(alpha = 0.15f),
                            Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f
                        )
                    }
                    Text(
                        "  masuk cepat  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnDeepBlue.copy(alpha = 0.35f)
                    )
                    Canvas(Modifier.weight(1f).height(1.dp)) {
                        drawLine(
                            OnDeepBlue.copy(alpha = 0.15f),
                            Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Biometric button — show whenever biometric hardware is available
                if (showBioButton) {
                    OutlinedButton(
                        onClick = {
                            if (pinManager.hasCachedProfile()) {
                                authViewModel.loginWithBiometric(
                                    activity = activity,
                                    onSuccess = { /* handled by LaunchedEffect(loginState) */ },
                                    onError = { showError = it }
                                )
                            } else {
                                showError = "Belum ada sesi tersimpan. Silakan login dengan email terlebih dahulu, lalu Anda dapat menggunakan biometrik untuk login cepat selanjutnya."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Gold, GoldLight))
                        )
                    ) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = Gold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Masuk dengan Biometrik",
                            color = OnDeepBlue.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // PIN button — show if PIN is set
                if (pinManager.isPinSet()) {
                    TextButton(
                        onClick = {
                            if (pinManager.hasCachedProfile()) {
                                showPinDialog = true
                            } else {
                                showError = "Belum ada sesi tersimpan. Silakan login dengan email terlebih dahulu."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Pin,
                            contentDescription = null,
                            tint = Gold.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Masuk dengan PIN",
                            color = Gold.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Setup PIN — always available
                    TextButton(
                        onClick = onNavigateToSetupPin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = Gold.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Setup PIN untuk Login Cepat",
                            color = Gold.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // ── Biometric Status Card ──
                // Shows device biometric capability with color-coded status
                if (biometricStatus != null) {
                    Spacer(Modifier.height(8.dp))

                    val statusColor = if (biometricStatus?.isAvailable == true)
                        StatusSuccess.copy(alpha = 0.15f)
                    else
                        Gold.copy(alpha = 0.1f)

                    val statusIcon = if (biometricStatus?.isAvailable == true)
                        Icons.Filled.CheckCircle
                    else
                        Icons.Filled.Info

                    val statusIconTint = if (biometricStatus?.isAvailable == true)
                        StatusSuccess
                    else
                        Gold

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = statusColor
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                statusIcon,
                                contentDescription = null,
                                tint = statusIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    biometricStatus?.userFriendlyMessage ?: "Memeriksa biometrik...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnDeepBlue.copy(alpha = 0.8f)
                                )
                                if (biometricStatus?.isNotEnrolled == true) {
                                    Text(
                                        "Buka Pengaturan → Keamanan → Daftarkan sidik jari",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnDeepBlue.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Debug seed button — only visible in debug builds
            if (BuildConfig.DEBUG) {
                TextButton(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val results = SeedUsers.seed()
                        withContext(Dispatchers.Main) {
                            showError = results.joinToString("\n")
                        }
                    }
                }) {
                    Text(
                        "[Dev] Seed Test Users",
                        color = OnDeepBlue.copy(alpha = 0.25f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    showError?.let { error ->
        ErrorDialog(message = error, onDismiss = { showError = null })
    }

    // ── PIN Login Dialog ──
    if (showPinDialog) {
        PinLoginDialog(
            authViewModel = authViewModel,
            onDismiss = {
                showPinDialog = false
                authViewModel.resetPinLockout()
            }
        )
    }
}

/**
 * Dialog for PIN-based quick login.
 * Shows 4-dot PIN display + numeric keypad.
 */
@Composable
private fun PinLoginDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val (failedAttempts, isLocked) = authViewModel.pinAttemptState

    // Reset error when PIN changes
    LaunchedEffect(pinInput) {
        if (pinInput.isNotEmpty()) errorMsg = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Masukkan PIN",
                    fontWeight = FontWeight.Bold,
                    color = OnDeepBlue
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Masukkan PIN 4 digit Anda",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnDeepBlue.copy(alpha = 0.55f)
                )

                Spacer(Modifier.height(20.dp))

                // PIN Display (4 dots)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { index ->
                        val isFilled = index < pinInput.length
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFilled) Gold.copy(alpha = 0.2f)
                                    else DeepBlueDark.copy(alpha = 0.3f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        if (isFilled) Gold else OnDeepBlue.copy(alpha = 0.15f),
                                        if (isFilled) GoldLight else OnDeepBlue.copy(alpha = 0.1f)
                                    )
                                )
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isFilled) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Gold, RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Error message
                if (errorMsg != null || failedAttempts > 0) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMsg ?: if (isLocked)
                            "Akun terkunci. Silakan login dengan email."
                        else
                            "",
                        color = StatusError,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Numeric Keypad
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1", "2", "3").forEach { digit ->
                            PinDialogKey(digit = digit, enabled = !isLocked && pinInput.length < 4) {
                                pinInput += digit
                                if (pinInput.length == 4) {
                                    authViewModel.loginWithPin(
                                        pin = pinInput,
                                        onSuccess = { onDismiss() },
                                        onError = { msg ->
                                            errorMsg = msg
                                            pinInput = ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("4", "5", "6").forEach { digit ->
                            PinDialogKey(digit = digit, enabled = !isLocked && pinInput.length < 4) {
                                pinInput += digit
                                if (pinInput.length == 4) {
                                    authViewModel.loginWithPin(
                                        pin = pinInput,
                                        onSuccess = { onDismiss() },
                                        onError = { msg ->
                                            errorMsg = msg
                                            pinInput = ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("7", "8", "9").forEach { digit ->
                            PinDialogKey(digit = digit, enabled = !isLocked && pinInput.length < 4) {
                                pinInput += digit
                                if (pinInput.length == 4) {
                                    authViewModel.loginWithPin(
                                        pin = pinInput,
                                        onSuccess = { onDismiss() },
                                        onError = { msg ->
                                            errorMsg = msg
                                            pinInput = ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Clear
                        Button(
                            onClick = { pinInput = ""; errorMsg = null },
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepBlueDark.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("C", fontWeight = FontWeight.Bold, color = OnDeepBlue.copy(alpha = 0.6f))
                        }

                        // 0
                        PinDialogKey(digit = "0", enabled = !isLocked && pinInput.length < 4) {
                            pinInput += "0"
                            if (pinInput.length == 4) {
                                authViewModel.loginWithPin(
                                    pin = pinInput,
                                    onSuccess = { onDismiss() },
                                    onError = { msg ->
                                        errorMsg = msg
                                        pinInput = ""
                                    }
                                )
                            }
                        }

                        // Delete
                        Button(
                            onClick = {
                                if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                errorMsg = null
                            },
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepBlueDark.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Hapus",
                                tint = OnDeepBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Gold.copy(alpha = 0.7f))
            }
        },
        containerColor = DeepBlueDark,
        titleContentColor = OnDeepBlue,
        textContentColor = OnDeepBlue.copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Small key button for the PIN login dialog.
 */
@Composable
private fun PinDialogKey(
    digit: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepBlueDark.copy(alpha = if (enabled) 0.5f else 0.25f),
            disabledContainerColor = DeepBlueDark.copy(alpha = 0.2f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) OnDeepBlue else OnDeepBlue.copy(alpha = 0.25f)
        )
    }
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
