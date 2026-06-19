package com.example.elsuarku.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.elsuarku.data.SeedUsers
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue
import com.example.elsuarku.ui.theme.SoftWhite
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

    // Google Sign-In client
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
            .background(DeepBlueDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Branding
            Text(
                text = "ElSuarKu",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = OnDeepBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aman, Transparan, Cepat, dan Terpercaya",
                style = MaterialTheme.typography.bodyMedium,
                color = OnDeepBlue.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ---- Google Sign-In Button ----
            OutlinedButton(
                onClick = {
                    isGoogleSigningIn = true
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isGoogleSigningIn && loginState !is AuthViewModel.LoginUiState.Loading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGoogleSigningIn) {
                    LoadingIndicator()
                } else {
                    Text("G", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = SoftWhite)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Masuk dengan Google", color = OnDeepBlue, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- Divider ----
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
                    drawLine(OnDeepBlue.copy(alpha = 0.2f), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
                }
                Text(
                    "  atau masuk dengan email  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnDeepBlue.copy(alpha = 0.5f)
                )
                Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
                    drawLine(OnDeepBlue.copy(alpha = 0.2f), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- Email Field ----
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Filled.Email, "Email", tint = OnDeepBlue.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Password Field ----
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Kata Sandi") },
                leadingIcon = { Icon(Icons.Filled.Lock, "Password", tint = OnDeepBlue.copy(alpha = 0.7f)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            "Toggle password",
                            tint = OnDeepBlue.copy(alpha = 0.7f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                singleLine = true,
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Login Button ----
            Button(
                onClick = { authViewModel.signInWithEmail(email, password) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = email.isNotBlank() && password.isNotBlank() && loginState !is AuthViewModel.LoginUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loginState is AuthViewModel.LoginUiState.Loading) {
                    LoadingIndicator()
                } else {
                    Text("Masuk", style = MaterialTheme.typography.labelLarge, color = DeepBlueDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Register Link ----
            TextButton(onClick = onNavigateToRegister) {
                Text("Belum punya akun? Daftar di sini", color = OnDeepBlue.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Debug: Seed Test Users ----
            TextButton(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    val results = SeedUsers.seed()
                    withContext(Dispatchers.Main) {
                        showError = results.joinToString("\n")
                    }
                }
            }) {
                Text("[Dev] Seed Test Users", color = OnDeepBlue.copy(alpha = 0.35f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    // Error dialog
    showError?.let { error ->
        ErrorDialog(message = error, onDismiss = { showError = null })
    }
}

@Composable
internal fun authTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = OnDeepBlue,
    unfocusedTextColor = OnDeepBlue.copy(alpha = 0.8f),
    focusedBorderColor = Gold,
    unfocusedBorderColor = OnDeepBlue.copy(alpha = 0.3f),
    focusedLabelColor = Gold,
    unfocusedLabelColor = OnDeepBlue.copy(alpha = 0.6f),
    cursorColor = Gold
)
