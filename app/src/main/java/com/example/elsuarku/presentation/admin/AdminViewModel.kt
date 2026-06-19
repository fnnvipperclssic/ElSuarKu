package com.example.elsuarku.presentation.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.*
import com.example.elsuarku.data.repository.*
import com.example.elsuarku.security.InputSanitizer
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val electionRepository: ElectionRepository,
    private val candidateRepository: CandidateRepository,
    private val voteRepository: VoteRepository,
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository,
    private val imageStorage: ImageStorage,
    private val sessionManager: SessionManager
) : ViewModel() {

    // === Admin State ===
    data class AdminState(
        // Election
        val elections: List<Election> = emptyList(),
        val selectedElection: Election? = null,
        // Candidate
        val candidates: List<Candidate> = emptyList(),
        // User Management
        val users: List<User> = emptyList(),
        val selectedUser: User? = null,
        // Stats
        val totalElections: Int = 0,
        val totalCandidates: Int = 0,
        val totalVoters: Int = 0,
        val totalVotes: Int = 0,
        val participationRate: Float = 0f,
        // Security
        val securityAlerts: List<AuditLog> = emptyList(),
        val failedLogins: Int = 0,
        // UI
        val isLoading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null
    )

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    val userId get() = sessionManager.getUserId() ?: ""
    private val userName get() = sessionManager.getUserName()
    private val userRole get() = sessionManager.getUserRole()?.name ?: "ADMIN"

    init { loadAllData() }

    // ==================== DATA LOADING ====================

    fun loadAllData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            loadElections()
            loadUsers()
            loadStats()
            loadSecurityAlerts()
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private suspend fun loadElections() {
        when (val r = electionRepository.getActiveElections()) {
            is Resource.Success -> _state.value = _state.value.copy(elections = r.data, totalElections = r.data.size)
            is Resource.Error -> _state.value = _state.value.copy(error = r.message)
            else -> {}
        }
    }

    private suspend fun loadUsers() {
        when (val r = authRepository.getAllUsers()) {
            is Resource.Success -> {
                _state.value = _state.value.copy(users = r.data, totalVoters = r.data.size)
            }
            else -> {}
        }
    }

    private suspend fun loadStats() {
        when (val r = voteRepository.getTotalVotesCount()) {
            is Resource.Success -> {
                val votes = r.data
                val voters = _state.value.totalVoters
                _state.value = _state.value.copy(
                    totalVotes = votes,
                    participationRate = if (voters > 0) votes.toFloat() / voters * 100f else 0f
                )
            }
            else -> {}
        }
    }

    private suspend fun loadSecurityAlerts() {
        when (val r = auditRepository.getLogsBySeverity(AuditSeverity.CRITICAL, 30)) {
            is Resource.Success -> _state.value = _state.value.copy(securityAlerts = r.data)
            else -> {}
        }
    }

    // ==================== ELECTION MANAGEMENT ====================

    fun createElection(title: String, description: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            // Sanitize inputs against injection
            val safeTitle = InputSanitizer.sanitizeText(title, 200)
            val safeDesc = InputSanitizer.sanitizeText(description, 2000)
            if (safeTitle.isBlank()) { _state.value = _state.value.copy(error = "Judul tidak valid"); return@launch }
            _state.value = _state.value.copy(isLoading = true)
            val election = Election(title = safeTitle, description = safeDesc, startDate = startDate, endDate = endDate, status = ElectionStatus.ACTIVE, createdBy = userId)
            when (val r = electionRepository.createElection(election)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.ELECTION_CREATED, r.data.id, title)
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Pemilihan berhasil dibuat")
                    loadAllData()
                }
                is Resource.Error -> _state.value = _state.value.copy(isLoading = false, error = r.message)
                else -> {}
            }
        }
    }

    fun updateElectionStatus(electionId: String, status: ElectionStatus) {
        viewModelScope.launch {
            when (val r = electionRepository.updateElectionStatus(electionId, status)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.ELECTION_UPDATED, electionId)
                    _state.value = _state.value.copy(successMessage = "Status pemilihan diperbarui")
                    loadAllData()
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    fun deleteElection(electionId: String) {
        viewModelScope.launch {
            when (val r = electionRepository.deleteElection(electionId)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.ELECTION_DELETED, electionId)
                    _state.value = _state.value.copy(successMessage = "Pemilihan dihapus")
                    loadAllData()
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    // ==================== CANDIDATE MANAGEMENT ====================

    fun loadCandidates(electionId: String) {
        viewModelScope.launch {
            when (val er = electionRepository.getElection(electionId)) {
                is Resource.Success -> _state.value = _state.value.copy(selectedElection = er.data)
                else -> {}
            }
            when (val r = candidateRepository.getCandidates(electionId)) {
                is Resource.Success -> _state.value = _state.value.copy(candidates = r.data, totalCandidates = r.data.size)
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    fun createCandidate(electionId: String, name: String, description: String, visi: String, misi: String, nomorUrut: Int, photoUri: Uri?) {
        viewModelScope.launch {
            // Sanitize all text inputs against injection
            val safeName = InputSanitizer.sanitizeText(name, 150)
            val safeDesc = InputSanitizer.sanitizeText(description, 1000)
            val safeVisi = InputSanitizer.sanitizeText(visi, 2000)
            val safeMisi = InputSanitizer.sanitizeText(misi, 2000)
            if (safeName.isBlank()) { _state.value = _state.value.copy(error = "Nama kandidat tidak valid"); return@launch }
            val photoBase64 = photoUri?.let { imageStorage.uriToBase64(it) } ?: ""
            val candidate = Candidate(electionId = electionId, name = safeName, photoBase64 = photoBase64, description = safeDesc, visi = safeVisi, misi = safeMisi, nomorUrut = nomorUrut)
            when (val r = candidateRepository.createCandidate(candidate)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.CANDIDATE_CREATED, electionId, name)
                    _state.value = _state.value.copy(successMessage = "Kandidat berhasil ditambahkan")
                    loadCandidates(electionId)
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    fun updateCandidateInfo(candidate: Candidate) {
        viewModelScope.launch {
            when (val r = candidateRepository.updateCandidate(candidate)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.CANDIDATE_UPDATED, candidate.id, candidate.name)
                    _state.value = _state.value.copy(successMessage = "Kandidat diperbarui")
                    candidate.electionId.let { loadCandidates(it) }
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    fun deleteCandidate(candidateId: String) {
        viewModelScope.launch {
            when (val r = candidateRepository.deleteCandidate(candidateId)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.CANDIDATE_DELETED, candidateId)
                    _state.value = _state.value.copy(successMessage = "Kandidat dihapus")
                    _state.value.selectedElection?.let { loadCandidates(it.id) }
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    // ==================== USER MANAGEMENT ====================

    fun updateUserRole(uid: String, newRole: UserRole) {
        viewModelScope.launch {
            when (val r = authRepository.updateUserRole(uid, newRole)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.USER_ACTIVATED, uid, "Role: $newRole")
                    _state.value = _state.value.copy(successMessage = "Role pengguna diperbarui")
                    loadUsers()
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    fun updateUserStatus(uid: String, status: UserStatus) {
        viewModelScope.launch {
            when (val r = authRepository.updateUserStatus(uid, status)) {
                is Resource.Success -> {
                    val action = if (status == UserStatus.SUSPENDED) AuditAction.USER_SUSPENDED else AuditAction.USER_ACTIVATED
                    auditRepository.log(userId, userName, userRole, action, uid)
                    _state.value = _state.value.copy(successMessage = "Status pengguna diperbarui")
                    loadUsers()
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    // ==================== REPORT ====================

    fun getElectionReport(electionId: String): String {
        val election = _state.value.elections.find { it.id == electionId } ?: return "Pemilihan tidak ditemukan"
        val candidates = _state.value.candidates
        val sb = StringBuilder()
        sb.appendLine("===== LAPORAN PEMILIHAN =====")
        sb.appendLine("Judul: ${election.title}")
        sb.appendLine("Deskripsi: ${election.description}")
        sb.appendLine("Total Pemilih: ${election.totalVoters}")
        sb.appendLine("Suara Masuk: ${election.votedCount}")
        sb.appendLine("Partisipasi: ${if (election.totalVoters > 0) (election.votedCount.toFloat() / election.totalVoters * 100) else 0f}%")
        sb.appendLine("Status: ${election.status.name}")
        sb.appendLine("")
        sb.appendLine("===== HASIL KANDIDAT =====")
        candidates.forEach { c ->
            sb.appendLine("${c.nomorUrut}. ${c.name} — ${c.voteCount} suara")
        }
        return sb.toString()
    }

    fun exportReport(electionId: String): String = getElectionReport(electionId)

    // ==================== UTILITY ====================

    fun clearMessages() { _state.value = _state.value.copy(error = null, successMessage = null) }
}
