package com.example.elsuarku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.elsuarku.data.model.UserRole
import com.example.elsuarku.di.AppModule
import com.example.elsuarku.navigation.ElSuarKuNavGraph
import com.example.elsuarku.navigation.Screen
import com.example.elsuarku.presentation.auth.AuthViewModel
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.security.ScreenProtection
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.ElSuarKuTheme

/**
 * ElSuarKu — Cloud-Based Secure E-Voting Platform
 * Single Activity, Compose-first architecture.
 */
class MainActivity : ComponentActivity() {

    private lateinit var appModule: AppModule
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable screen protection for the entire app
        ScreenProtection.enable(this)

        // Initialize DI
        appModule = AppModule(applicationContext)
        authViewModel = appModule.authViewModel()

        enableEdgeToEdge()

        setContent {
            ElSuarKuTheme {
                ElSuarKuApp(authViewModel, appModule)
            }
        }
    }
}

@Composable
private fun ElSuarKuApp(
    authViewModel: AuthViewModel,
    appModule: AppModule
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // Auto-redirect on auth state change
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthViewModel.AuthUiState.Authenticated -> {
                val destination = when (state.role) {
                    UserRole.ADMIN -> Screen.AdminDashboard.route
                    UserRole.MONITOR -> Screen.MonitorDashboard.route
                    else -> Screen.UserHome.route
                }
                navController.navigate(destination) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthViewModel.AuthUiState.NotAuthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthViewModel.AuthUiState.Loading -> {
                // Still loading — splash is handled below
            }
        }
    }

    // Show Splash while loading, NavGraph otherwise
    if (authState is AuthViewModel.AuthUiState.Loading) {
        SplashScreen()
    } else {
        ElSuarKuNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            appModule = appModule
        )
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlueDark),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(message = "Memuat ElSuarKu…")
    }
}
