package com.example.elsuarku.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.elsuarku.di.AppModule
import com.example.elsuarku.presentation.admin.*
import com.example.elsuarku.presentation.auth.*
import com.example.elsuarku.presentation.dashboard.*
import com.example.elsuarku.presentation.monitor.*
import com.example.elsuarku.presentation.settings.*
import com.example.elsuarku.presentation.voting.*

/**
 * Creates a ViewModelProvider.Factory from a lambda.
 * Ensures ViewModels survive configuration changes (rotation, etc.)
 * instead of being recreated by remember{}.
 */
private inline fun <reified T : ViewModel> viewModelFactory(crossinline creator: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            if (modelClass.isAssignableFrom(T::class.java)) {
                return creator() as VM
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@Composable
fun ElSuarKuNavGraph(navController: NavHostController, authViewModel: AuthViewModel, appModule: AppModule) {
    // S5: Use viewModel() instead of remember{} so ViewModels survive config changes (rotation, etc.)
    // Factory creates a new instance per screen-level scope
    val votingViewModel: VotingViewModel = viewModel(factory = viewModelFactory { appModule.votingViewModel() })
    val adminViewModel: AdminViewModel = viewModel(factory = viewModelFactory { appModule.adminViewModel() })
    val monitorViewModel: MonitorViewModel = viewModel(factory = viewModelFactory { appModule.monitorViewModel() })
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory { appModule.dashboardViewModel() })

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        // ==================== AUTH ====================
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { roleName ->
                    val destination = when (roleName) {
                        "ADMIN" -> Screen.AdminDashboard.route
                        "MONITOR" -> Screen.MonitorDashboard.route
                        else -> Screen.UserHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = { roleName ->
                    val destination = when (roleName) {
                        "ADMIN" -> Screen.AdminDashboard.route
                        "MONITOR" -> Screen.MonitorDashboard.route
                        else -> Screen.UserHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ==================== USER (PEMILIH) ====================
        composable(Screen.UserHome.route) {
            UserDashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToCandidates = { eid -> navController.navigate(Screen.CandidateDetail.createRoute(eid)) },
                onNavigateToMyVoting = { navController.navigate(Screen.MyVoting.route) },
                onNavigateToProfile = { navController.navigate(Screen.UserProfile.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToElectionList = { navController.navigate(Screen.ElectionList.route) },
                onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }
            )
        }
        composable(Screen.ElectionList.route) { ElectionListScreen(viewModel = votingViewModel, onCandidateClick = { eid -> navController.navigate(Screen.CandidateDetail.createRoute(eid)) }, onBack = { navController.popBackStack() }) }
        composable(Screen.CandidateDetail.route, arguments = listOf(navArgument("electionId") { type = NavType.StringType })) { be -> val eid = be.arguments?.getString("electionId") ?: ""; CandidateDetailScreen(electionId = eid, viewModel = votingViewModel, onVoteClick = { cid -> navController.navigate(Screen.VoteConfirmation.createRoute(eid, cid)) }, onBack = { navController.popBackStack() }) }
        composable(Screen.VoteConfirmation.route, arguments = listOf(navArgument("electionId") { type = NavType.StringType }, navArgument("candidateId") { type = NavType.StringType })) { be -> VoteConfirmationScreen(electionId = be.arguments?.getString("electionId") ?: "", candidateId = be.arguments?.getString("candidateId") ?: "", viewModel = votingViewModel, onVoteSuccess = { navController.navigate(Screen.VoteSuccess.route) { popUpTo(Screen.ElectionList.route) } }, onBack = { navController.popBackStack() }) }
        composable(Screen.VoteSuccess.route) { VoteSuccessScreen(viewModel = votingViewModel, onBackToDashboard = { navController.navigate(Screen.UserHome.route) { popUpTo(Screen.UserHome.route) { inclusive = true } } }) }
        composable(Screen.MyVoting.route) { MyVotingScreen(viewModel = dashboardViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.UserProfile.route) { UserProfileScreen(viewModel = dashboardViewModel, authViewModel = authViewModel, sessionManager = appModule.sessionManager, imageStorage = appModule.imageStorage, onBack = { navController.popBackStack() }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }) }

        // ==================== ADMIN ====================
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(viewModel = adminViewModel, onNavigateToElections = { navController.navigate(Screen.ManageElection.route) }, onNavigateToUserManagement = { navController.navigate(Screen.UserManagement.route) }, onNavigateToReport = { navController.navigate(Screen.ReportCenter.route) }, onNavigateToSecurity = { navController.navigate(Screen.AdminSecurity.route) }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } })
        }
        composable(Screen.ManageElection.route) { ManageElectionScreen(viewModel = adminViewModel, onManageCandidates = { eid -> navController.navigate(Screen.ManageCandidate.createRoute(eid)) }, onBack = { navController.popBackStack() }) }
        composable(Screen.ManageCandidate.route, arguments = listOf(navArgument("electionId") { type = NavType.StringType })) { be -> ManageCandidateScreen(electionId = be.arguments?.getString("electionId") ?: "", viewModel = adminViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.UserManagement.route) { UserManagementScreen(viewModel = adminViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.ReportCenter.route) { ReportCenterScreen(viewModel = adminViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.AdminSecurity.route) { SecurityCenterScreen(viewModel = adminViewModel, onBack = { navController.popBackStack() }) }

        // ==================== MONITOR ====================
        composable(Screen.MonitorDashboard.route) {
            MonitorDashboardScreen(viewModel = monitorViewModel, onNavigateToLiveStats = { eid -> navController.navigate(Screen.LiveStats.createRoute(eid)) }, onNavigateToAuditLogs = { navController.navigate(Screen.AuditLogs.route) }, onNavigateToSecurity = { navController.navigate(Screen.SecurityMonitor.route) }, onNavigateToReport = { navController.navigate(Screen.MonitorReport.route) }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } })
        }
        composable(Screen.LiveStats.route, arguments = listOf(navArgument("electionId") { type = NavType.StringType })) { be -> LiveStatsScreen(electionId = be.arguments?.getString("electionId") ?: "", viewModel = monitorViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.AuditLogs.route) { AuditLogScreen(viewModel = monitorViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SecurityMonitor.route) { SecurityMonitorScreen(viewModel = monitorViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.MonitorReport.route) { MonitorReportScreen(viewModel = monitorViewModel, onBack = { navController.popBackStack() }) }

        // ==================== COMMON ====================
        composable(Screen.Settings.route) { SettingsScreen(authViewModel = authViewModel, sessionManager = appModule.sessionManager, onBack = { navController.popBackStack() }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }) }
    }
}
