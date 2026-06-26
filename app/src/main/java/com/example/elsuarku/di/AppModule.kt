package com.example.elsuarku.di

import android.content.Context
import com.example.elsuarku.data.repository.AnnouncementRepository
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.data.repository.AuthRepository
import com.example.elsuarku.data.repository.CandidateRepository
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.ImageStorage
import com.example.elsuarku.data.repository.VoteRepository
import com.example.elsuarku.data.repository.ElectionTemplateRepository
import com.example.elsuarku.domain.repository.IAnnouncementRepository
import com.example.elsuarku.domain.repository.IAuditRepository
import com.example.elsuarku.domain.repository.IAuthRepository
import com.example.elsuarku.domain.repository.ICandidateRepository
import com.example.elsuarku.domain.repository.IElectionRepository
import com.example.elsuarku.domain.repository.IElectionTemplateRepository
import com.example.elsuarku.domain.repository.IVoteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.elsuarku.presentation.admin.AdminViewModel
import com.example.elsuarku.presentation.auth.AuthViewModel
import com.example.elsuarku.presentation.dashboard.DashboardViewModel
import com.example.elsuarku.domain.usecase.SubmitVoteUseCase
import com.example.elsuarku.presentation.monitor.MonitorViewModel
import com.example.elsuarku.presentation.voting.VotingViewModel
import com.example.elsuarku.security.AntiTampering
import com.example.elsuarku.security.EncryptionManager
import com.example.elsuarku.security.PinManager
import com.example.elsuarku.security.SessionManager

/**
 * Manual dependency injection module.
 *
 * Wires interfaces → concrete implementations for testability and DIP compliance.
 * In a larger app, consider using Hilt or Koin.
 * For ElSuarKu, manual DI keeps things explicit and avoids annotation processing overhead.
 */
class AppModule(context: Context) {

    // ---- Security ----
    val sessionManager = SessionManager(context)
    val encryptionManager = EncryptionManager()
    val antiTampering = AntiTampering(context)
    val pinManager = PinManager(context)

    // ---- Storage ----
    val imageStorage = ImageStorage(context)

    // ---- Repositories (wired via interfaces for testability) ----
    val authRepository: IAuthRepository by lazy { AuthRepository() }
    val electionRepository: IElectionRepository by lazy { ElectionRepository() }
    val candidateRepository: ICandidateRepository by lazy { CandidateRepository() }
    val voteRepository: IVoteRepository by lazy { VoteRepository() }
    val auditRepository: IAuditRepository by lazy { AuditRepository() }
    val announcementRepository: IAnnouncementRepository by lazy { AnnouncementRepository() }
    val electionTemplateRepository: IElectionTemplateRepository by lazy {
        ElectionTemplateRepository(FirebaseFirestore.getInstance())
    }

    // ---- Use Cases ----
    val submitVoteUseCase: SubmitVoteUseCase by lazy {
        SubmitVoteUseCase(voteRepository, candidateRepository, electionRepository, encryptionManager, sessionManager, auditRepository)
    }

    // ---- ViewModels (factory methods — new instance per call) ----
    fun authViewModel(): AuthViewModel =
        AuthViewModel(authRepository, sessionManager, auditRepository, pinManager)

    fun dashboardViewModel(): DashboardViewModel =
        DashboardViewModel(electionRepository, voteRepository, authRepository, sessionManager, announcementRepository)

    fun votingViewModel(): VotingViewModel =
        VotingViewModel(
            electionRepository,
            candidateRepository,
            voteRepository,
            encryptionManager,
            sessionManager,
            auditRepository,
            submitVoteUseCase
        )

    fun adminViewModel(): AdminViewModel =
        AdminViewModel(
            electionRepository,
            candidateRepository,
            voteRepository,
            auditRepository,
            authRepository,
            imageStorage,
            sessionManager,
            announcementRepository
        )

    fun monitorViewModel(): MonitorViewModel =
        MonitorViewModel(
            electionRepository,
            voteRepository,
            auditRepository,
            authRepository,
            candidateRepository,
            sessionManager
        )
}
