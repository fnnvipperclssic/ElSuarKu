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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elsuarku.data.SeedUsers
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
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
    onNavigateToRegister: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                    enabled = email.isNotBlank() && password.isNotBlank()
                            && loginState !is AuthViewModel.LoginUiState.Loading,
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
            }

            Spacer(Modifier.height(32.dp))

            // Debug seed button
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

            Spacer(Modifier.height(16.dp))
        }
    }

    showError?.let { error ->
        ErrorDialog(message = error, onDismiss = { showError = null })
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
