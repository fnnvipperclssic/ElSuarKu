package com.example.elsuarku.di

import android.content.Context
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.data.repository.AuthRepository
import com.example.elsuarku.data.repository.CandidateRepository
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.ImageStorage
import com.example.elsuarku.data.repository.VoteRepository
import com.example.elsuarku.presentation.admin.AdminViewModel
import com.example.elsuarku.presentation.auth.AuthViewModel
import com.example.elsuarku.presentation.dashboard.DashboardViewModel
import com.example.elsuarku.presentation.monitor.MonitorViewModel
import com.example.elsuarku.presentation.voting.VotingViewModel
import com.example.elsuarku.security.AntiTampering
import com.example.elsuarku.security.EncryptionManager
import com.example.elsuarku.security.SessionManager

/**
 * Manual dependency injection module.
 *
 * Provides all singleton instances. In a larger app, consider using Hilt or Koin.
 * For ElSuarKu, manual DI keeps things explicit and avoids annotation processing overhead.
 */
class AppModule(context: Context) {

    // ---- Security ----
    val sessionManager = SessionManager(context)
    val encryptionManager = EncryptionManager()
    val antiTampering = AntiTampering(context)

    // ---- Storage ----
    val imageStorage = ImageStorage(context)

    // ---- Repositories ----
    val authRepository = AuthRepository()
    val electionRepository = ElectionRepository()
    val candidateRepository = CandidateRepository()
    val voteRepository = VoteRepository()
    val auditRepository = AuditRepository()

    // ---- ViewModels (factory methods — new instance per call) ----
    fun authViewModel(): AuthViewModel =
        AuthViewModel(authRepository, sessionManager, auditRepository)

    fun dashboardViewModel(): DashboardViewModel =
        DashboardViewModel(electionRepository, voteRepository, sessionManager)

    fun votingViewModel(): VotingViewModel =
        VotingViewModel(
            electionRepository,
            candidateRepository,
            voteRepository,
            encryptionManager,
            sessionManager,
            auditRepository
        )

    fun adminViewModel(): AdminViewModel =
        AdminViewModel(
            electionRepository,
            candidateRepository,
            voteRepository,
            auditRepository,
            authRepository,
            imageStorage,
            sessionManager
        )

    fun monitorViewModel(): MonitorViewModel =
        MonitorViewModel(
            electionRepository,
            voteRepository,
            auditRepository,
            sessionManager
        )
}
