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
        // Identity
        val adminName: String = "",
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

    init {
        _state.value = _state.value.copy(adminName = sessionManager.getUserName())
        loadAllData()
    }

    // ==================== DATA LOADING ====================

    fun loadAllData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                loadElections()
                loadUsers()
                loadStats()
                loadSecurityAlerts()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal memuat data: ${e.localizedMessage ?: "kesalahan tidak diketahui"}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadElections() {
        when (val r = electionRepository.getAllElections()) {
            is Resource.Success -> {
                // Load total candidates across all elections
                var totalCandidates = 0
                for (election in r.data) {
                    when (val cr = candidateRepository.getCandidates(election.id)) {
                        is Resource.Success -> totalCandidates += cr.data.size
                        else -> {}
                    }
                }
                _state.value = _state.value.copy(
                    elections = r.data,
                    totalElections = r.data.size,
                    totalCandidates = totalCandidates
                )
            }
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
        var critical: List<AuditLog> = emptyList()
        var warnings: List<AuditLog> = emptyList()
        var info: List<AuditLog> = emptyList()

        when (val r = auditRepository.getLogsBySeverity(AuditSeverity.CRITICAL, 30)) {
            is Resource.Success -> critical = r.data
            else -> {}
        }
        when (val r = auditRepository.getLogsBySeverity(AuditSeverity.WARNING, 30)) {
            is Resource.Success -> warnings = r.data
            else -> {}
        }
        when (val r = auditRepository.getLogsBySeverity(AuditSeverity.INFO, 30)) {
            is Resource.Success -> info = r.data
            else -> {}
        }

        _state.value = _state.value.copy(securityAlerts = critical + warnings + info)
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

    fun updateElection(electionId: String, title: String, description: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val safeTitle = InputSanitizer.sanitizeText(title, 200)
            val safeDesc = InputSanitizer.sanitizeText(description, 2000)
            if (safeTitle.isBlank()) { _state.value = _state.value.copy(error = "Judul tidak valid"); return@launch }
            _state.value = _state.value.copy(isLoading = true)
            val updated = Election(id = electionId, title = safeTitle, description = safeDesc, startDate = startDate, endDate = endDate, status = ElectionStatus.ACTIVE, createdBy = userId)
            when (val r = electionRepository.updateElection(updated)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.ELECTION_UPDATED, electionId, title)
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Pemilihan berhasil diperbarui")
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
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val er = electionRepository.getElection(electionId)) {
                is Resource.Success -> _state.value = _state.value.copy(selectedElection = er.data)
                else -> {}
            }
            when (val r = candidateRepository.getCandidates(electionId)) {
                is Resource.Success -> _state.value = _state.value.copy(candidates = r.data, totalCandidates = r.data.size, isLoading = false)
                is Resource.Error -> _state.value = _state.value.copy(error = r.message, isLoading = false)
                else -> {}
            }
        }
    }

    fun createCandidate(electionId: String, name: String, description: String, visi: String, misi: String, nomorUrut: Int, photoUri: Uri?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            // Sanitize all text inputs against injection
            val safeName = InputSanitizer.sanitizeText(name, 150)
            val safeDesc = InputSanitizer.sanitizeText(description, 1000)
            val safeVisi = InputSanitizer.sanitizeText(visi, 2000)
            val safeMisi = InputSanitizer.sanitizeText(misi, 2000)
            if (safeName.isBlank()) { _state.value = _state.value.copy(isLoading = false, error = "Nama kandidat tidak valid"); return@launch }
            val photoBase64 = photoUri?.let { imageStorage.uriToBase64(it) } ?: ""
            if (photoBase64.isNotBlank() && !imageStorage.isWithinSizeLimit(photoBase64)) {
                _state.value = _state.value.copy(isLoading = false, error = "Foto kandidat terlalu besar (maks ~900KB). Pilih foto lebih kecil.")
                return@launch
            }
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

    fun updateCandidatePhoto(candidateId: String, photoUri: Uri) {
        viewModelScope.launch {
            val photoBase64 = imageStorage.uriToBase64(photoUri) ?: run {
                _state.value = _state.value.copy(error = "Gagal memproses gambar. Coba foto lain.")
                return@launch
            }
            if (!imageStorage.isWithinSizeLimit(photoBase64)) {
                _state.value = _state.value.copy(error = "Foto terlalu besar (maks ~900KB). Pilih foto lebih kecil.")
                return@launch
            }
            when (val result = candidateRepository.getCandidate(candidateId)) {
                is Resource.Success -> {
                    val updated = result.data.copy(photoBase64 = photoBase64)
                    when (val r = candidateRepository.updateCandidate(updated)) {
                        is Resource.Success -> {
                            _state.value = _state.value.copy(successMessage = "Foto kandidat diperbarui")
                            result.data.electionId.let { loadCandidates(it) }
                        }
                        is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                        else -> {}
                    }
                }
                is Resource.Error -> _state.value = _state.value.copy(error = result.message)
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
