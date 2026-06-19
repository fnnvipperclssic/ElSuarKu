package com.example.elsuarku.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
fun ElSuarKuNavGraph(navController: NavHostController, authViewModel: AuthViewModel, appModule: AppModule) {
    val votingViewModel = remember { appModule.votingViewModel() }
    val adminViewModel = remember { appModule.adminViewModel() }
    val monitorViewModel = remember { appModule.monitorViewModel() }
    val dashboardViewModel = remember { appModule.dashboardViewModel() }

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        // ==================== AUTH ====================
        composable(Screen.Login.route) { LoginScreen(authViewModel = authViewModel, onLoginSuccess = {}, onNavigateToRegister = { navController.navigate(Screen.Register.route) }) }
        composable(Screen.Register.route) { RegisterScreen(authViewModel = authViewModel, onRegisterSuccess = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Login.route) { inclusive = true } } }, onNavigateBack = { navController.popBackStack() }) }

        // ==================== USER (PEMILIH) ====================
        composable(Screen.UserHome.route) {
            UserDashboardScreen(viewModel = dashboardViewModel, onNavigateToCandidates = { eid -> navController.navigate(Screen.CandidateDetail.createRoute(eid)) }, onNavigateToMyVoting = { navController.navigate(Screen.MyVoting.route) }, onNavigateToProfile = { navController.navigate(Screen.UserProfile.route) }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } })
        }
        composable(Screen.ElectionList.route) { ElectionListScreen(viewModel = votingViewModel, onCandidateClick = { eid -> navController.navigate(Screen.CandidateDetail.createRoute(eid)) }, onBack = { navController.popBackStack() }) }
        composable(Screen.CandidateDetail.route, arguments = listOf(navArgument("electionId") { type = NavType.StringType })) { be -> val eid = be.arguments?.getString("electionId") ?: ""; CandidateDetailScreen(electionId = eid, viewModel = votingViewModel, onVoteClick = { cid -> navController.navigate(Screen.VoteConfirmation.createRoute(eid, cid)) }, onBack = { navController.popBackStack() }) }
        composable(Screen.VoteConfirmation.route, arguments = listOf(navArgument("electionId") { type = NavType.StringType }, navArgument("candidateId") { type = NavType.StringType })) { be -> VoteConfirmationScreen(electionId = be.arguments?.getString("electionId") ?: "", candidateId = be.arguments?.getString("candidateId") ?: "", viewModel = votingViewModel, onVoteSuccess = { navController.navigate(Screen.VoteSuccess.route) { popUpTo(Screen.ElectionList.route) } }, onBack = { navController.popBackStack() }) }
        composable(Screen.VoteSuccess.route) { VoteSuccessScreen(onBackToDashboard = { navController.navigate(Screen.UserHome.route) { popUpTo(Screen.UserHome.route) { inclusive = true } } }) }
        composable(Screen.MyVoting.route) { MyVotingScreen(viewModel = dashboardViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.UserProfile.route) { UserProfileScreen(viewModel = dashboardViewModel, authViewModel = authViewModel, sessionManager = appModule.sessionManager, onBack = { navController.popBackStack() }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }) }

        // ==================== ADMIN ====================
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(viewModel = adminViewModel, onNavigateToElections = { navController.navigate(Screen.ManageElection.route) }, onNavigateToUserManagement = { navController.navigate(Screen.UserManagement.route) }, onNavigateToReport = { navController.navigate(Screen.ReportCenter.route) }, onNavigateToSecurity = { navController.navigate(Screen.AdminSecurity.route) }, onNavigateToSettings = { navController.navigate(Screen.Settings.route) }, onLogout = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } })
        }
        composable(Screen.ManageElection.route) { ManageElectionScreen(viewModel = adminViewModel, onManageCandidates = { eid -> navController.navigate(Screen.ManageCandidate.createRoute(eid)) }, onBack = { navController.popBackStack() }) }
        composable(Screen.ManageCandidate.route, arguments = listOf(navArgument("electionId") { type = NavType.StringType })) { be -> ManageCandidateScreen(electionId = be.arguments?.getString("electionId") ?: "", viewModel = adminViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.UserManagement.route) { UserManagementScreen(viewModel = adminViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.ReportCenter.route) { ReportCenterScreen(viewModel = adminViewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.AdminSecurity.route) { SecurityCenterScreen(viewModel = adminViewModel, auditRepository = appModule.auditRepository, onBack = { navController.popBackStack() }) }

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
