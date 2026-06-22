package com.example.elsuarku.presentation.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.GlassCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.*

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    var registrationSuccess by remember { mutableStateOf(false) }

    val entryAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(400), label = "entry")

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthViewModel.LoginUiState.Success -> {
                registrationSuccess = true
                val role = (loginState as AuthViewModel.LoginUiState.Success).role
                onRegisterSuccess(role.name)
                authViewModel.resetLoginState()
            }
            is AuthViewModel.LoginUiState.Error -> {
                showError = (loginState as AuthViewModel.LoginUiState.Error).message
                authViewModel.resetLoginState()
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
                .alpha(entryAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Back arrow
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = OnDeepBlue.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Icon
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Gold
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Daftar Akun",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = OnDeepBlue
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Buat akun ElSuarKu Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = OnDeepBlue.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(28.dp))

            // Registration form card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isDark = true,
                cornerRadius = 20.dp
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, "Nama", tint = OnDeepBlue.copy(alpha = 0.6f))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    colors = authTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

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

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, "Password", tint = OnDeepBlue.copy(alpha = 0.6f))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    colors = authTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Konfirmasi Kata Sandi") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, "Konfirmasi", tint = OnDeepBlue.copy(alpha = 0.6f))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    colors = authTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            showError = "Kata sandi tidak cocok"
                        } else {
                            authViewModel.register(name, email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = name.isNotBlank() && email.isNotBlank()
                            && password.length >= 6 && password == confirmPassword
                            && loginState !is AuthViewModel.LoginUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loginState is AuthViewModel.LoginUiState.Loading) {
                        LoadingIndicator()
                    } else {
                        Text(
                            "Daftar",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = DeepBlueDark
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Sudah punya akun? Masuk",
                        color = Gold.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    showError?.let {
        ErrorDialog(message = it, onDismiss = { showError = null; registrationSuccess = false })
    }
}
