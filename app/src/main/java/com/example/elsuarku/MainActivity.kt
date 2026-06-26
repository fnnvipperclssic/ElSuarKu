package com.example.elsuarku

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
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
class MainActivity : FragmentActivity() {

    private lateinit var appModule: AppModule
    private lateinit var authViewModel: AuthViewModel

    // Permission launcher for Android 13+ notification permission
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — app continues either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable screen protection for the entire app
        ScreenProtection.enable(this)

        // Initialize DI
        appModule = AppModule(applicationContext)
        authViewModel = appModule.authViewModel()

        enableEdgeToEdge()

        setContent {
            // NOTE: requestNotificationPermission() is called in onResume()
            // (below) to avoid the permission dialog stealing window focus
            // during initial composition, which causes ANR.
            ElSuarKuTheme {
                ElSuarKuApp(authViewModel, appModule)
            }
        }
    }

    /**
     * Request POST_NOTIFICATIONS permission on Android 13+.
     * Shows a system dialog. If denied, the app still works — notifications are simply not shown.
     * User can re-enable in: Settings → Apps → ElSuarKu → Notifications
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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

    // Block all Firebase operations if Firebase init failed
    if (!ElSuarKuApp.firebaseInitialized) {
        val ctx = LocalContext.current
        FirebaseErrorScreen(onRetry = {
            // Restart the activity to retry Firebase initialization
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            (ctx as? android.app.Activity)?.finish()
        })
        return
    }

    // Track whether initial auth resolution has been handled.
    // Prevents race condition: MainActivity handles session restore (first resolution),
    // while LoginScreen/RegisterScreen handle active login/registration navigation.
    var hasHandledInitialAuth by remember { mutableStateOf(false) }

    // Auto-redirect on auth state change
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthViewModel.AuthUiState.Authenticated -> {
                // Only navigate on initial session restore.
                // Active login and registration flows handle their own navigation.
                if (!hasHandledInitialAuth) {
                    val destination = when (state.role) {
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                        UserRole.MONITOR -> Screen.MonitorDashboard.route
                        else -> Screen.UserHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                hasHandledInitialAuth = true
            }
            is AuthViewModel.AuthUiState.NotAuthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
                hasHandledInitialAuth = true
            }
            is AuthViewModel.AuthUiState.Loading -> {
                // Still loading — splash overlay shown below
            }
        }
    }

    // Determine start destination: show onboarding for first-time users
    val startDestination = if (!appModule.sessionManager.isOnboardingComplete()) {
        Screen.Onboarding.route
    } else {
        Screen.Login.route
    }

    // Always compose NavGraph so NavHost is available for navigation.
    // Splash is rendered as a full-screen overlay during loading.
    Box(modifier = Modifier.fillMaxSize()) {
        ElSuarKuNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            appModule = appModule,
            startDestination = startDestination
        )

        if (authState is AuthViewModel.AuthUiState.Loading) {
            SplashScreen()
        }
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

@Composable
private fun FirebaseErrorScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlueDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = androidx.compose.ui.graphics.Color(0xFFE53935)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Firebase initialization failed",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "The app requires Firebase services to function.\nPlease check your internet connection and Google Play Services.",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("Retry", fontWeight = FontWeight.Bold, color = DeepBlueDark)
            }
        }
    }
}
