package com.example.elsuarku.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object Register : Screen("register")

    // ===== USER (PEMILIH) =====
    data object UserHome : Screen("user_home")
    data object ElectionList : Screen("election_list")
    data object CandidateDetail : Screen("candidate_detail/{electionId}") {
        fun createRoute(electionId: String) = "candidate_detail/$electionId"
    }
    data object VoteConfirmation : Screen("vote_confirmation/{electionId}/{candidateId}") {
        fun createRoute(electionId: String, candidateId: String) = "vote_confirmation/$electionId/$candidateId"
    }
    data object VoteSuccess : Screen("vote_success")
    data object MyVoting : Screen("my_voting")
    data object UserProfile : Screen("user_profile")

    // ===== ADMIN =====
    data object AdminDashboard : Screen("admin_dashboard")
    data object ManageElection : Screen("manage_election")
    data object ManageCandidate : Screen("manage_candidate/{electionId}") {
        fun createRoute(electionId: String) = "manage_candidate/$electionId"
    }
    data object UserManagement : Screen("user_management")
    data object ReportCenter : Screen("report_center")
    data object AdminSecurity : Screen("admin_security")

    // ===== MONITOR =====
    data object MonitorDashboard : Screen("monitor_dashboard")
    data object LiveStats : Screen("live_stats/{electionId}") {
        fun createRoute(electionId: String) = "live_stats/$electionId"
    }
    data object AuditLogs : Screen("audit_logs")
    data object SecurityMonitor : Screen("security_monitor")
    data object MonitorReport : Screen("monitor_report")

    // Common
    data object Settings : Screen("settings")
}
