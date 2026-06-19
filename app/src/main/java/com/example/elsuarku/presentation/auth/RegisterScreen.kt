package com.example.elsuarku.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.ErrorDialog
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthViewModel.LoginUiState.Success -> {
                onRegisterSuccess()
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
            .background(DeepBlueDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = OnDeepBlue
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Daftar Akun",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = OnDeepBlue
            )
            Text(
                text = "Buat akun ElSuarKu Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = OnDeepBlue.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = "Nama", tint = OnDeepBlue.copy(alpha = 0.7f))
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Filled.Email, contentDescription = "Email", tint = OnDeepBlue.copy(alpha = 0.7f))
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Kata Sandi") },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = "Password", tint = OnDeepBlue.copy(alpha = 0.7f))
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Konfirmasi Kata Sandi") },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = "Konfirmasi", tint = OnDeepBlue.copy(alpha = 0.7f))
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                colors = authTextFieldColors(),
                isError = confirmPassword.isNotEmpty() && password != confirmPassword
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password != confirmPassword) {
                        showError = "Kata sandi tidak cocok"
                    } else {
                        authViewModel.register(name, email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = name.isNotBlank() && email.isNotBlank() &&
                        password.length >= 6 && password == confirmPassword &&
                        loginState !is AuthViewModel.LoginUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loginState is AuthViewModel.LoginUiState.Loading) {
                    LoadingIndicator()
                } else {
                    Text("Daftar", style = MaterialTheme.typography.labelLarge, color = DeepBlueDark)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateBack) {
                Text("Sudah punya akun? Masuk", color = OnDeepBlue.copy(alpha = 0.8f))
            }
        }
    }

    showError?.let {
        ErrorDialog(message = it, onDismiss = { showError = null })
    }
}
