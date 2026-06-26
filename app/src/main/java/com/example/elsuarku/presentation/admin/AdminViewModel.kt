package com.example.elsuarku.presentation.admin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.*
import com.example.elsuarku.data.repository.AuthRepository
import com.example.elsuarku.data.repository.ImageStorage
import com.example.elsuarku.domain.repository.IAnnouncementRepository
import com.example.elsuarku.domain.repository.IAuditRepository
import com.example.elsuarku.domain.repository.IAuthRepository
import com.example.elsuarku.domain.repository.ICandidateRepository
import com.example.elsuarku.domain.repository.IElectionRepository
import com.example.elsuarku.domain.repository.IVoteRepository
import com.example.elsuarku.security.InputSanitizer
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch

class AdminViewModel(
    private val electionRepository: IElectionRepository,
    private val candidateRepository: ICandidateRepository,
    private val voteRepository: IVoteRepository,
    private val auditRepository: IAuditRepository,
    private val authRepository: IAuthRepository,
    private val imageStorage: ImageStorage,
    private val sessionManager: SessionManager,
    private val announcementRepository: IAnnouncementRepository
) : ViewModel() {
    companion object {
        private const val TAG = "AdminViewModel"
    }

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
        // Announcements
        val announcements: List<Announcement> = emptyList(),
        val isAnnouncementLoading: Boolean = false,
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
    private val authRepo get() = authRepository as AuthRepository

    // ── Real-time observer jobs ──
    private var electionsJob: Job? = null
    private var usersJob: Job? = null
    private var securityJob: Job? = null
    private var recomputeJob: Job? = null
    private var observing = false

    init {
        // ONLY set identity info. Do NOT load data — user may not be logged in yet
        // (ViewModel is created at NavGraph level, before login).
        _state.value = _state.value.copy(
            adminName = sessionManager.getUserName(),
            isLoading = false
        )
    }

    // ==================== REAL-TIME OBSERVERS ====================

    /**
     * Start Firestore snapshot listeners for elections, users, and security logs.
     * Stats are recomputed whenever elections or users data changes.
     */
    private fun startRealTimeObservers() {
        _state.value = _state.value.copy(isLoading = true, error = null)

        electionsJob = viewModelScope.launch {
            electionRepository.observeAllElections().cancellable().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val elections = result.data
                        var totalCandidates = 0
                        for (election in elections) {
                            when (val cr = candidateRepository.getCandidates(election.id)) {
                                is Resource.Success -> totalCandidates += cr.data.size
                                else -> {}
                            }
                        }
                        _state.value = _state.value.copy(
                            elections = elections,
                            totalElections = elections.size,
                            totalCandidates = totalCandidates,
                            isLoading = false,
                            error = null
                        )
                        recomputeStats()
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    is Resource.Loading -> { /* no-op */ }
                    is Resource.Cached<*> -> { /* no-op */ }
                    is Resource.Empty -> { /* no-op */ }
                }
            }
        }

        usersJob = viewModelScope.launch {
            authRepository.observeAllUsers().cancellable().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            users = result.data,
                            totalVoters = result.data.size
                        )
                        recomputeStats()
                    }
                    else -> { /* keep stale data on error */ }
                }
            }
        }

        securityJob = viewModelScope.launch {
            auditRepository.observeRecentLogs(100).cancellable().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(securityAlerts = result.data)
                    }
                    else -> { /* keep stale data */ }
                }
            }
        }
    }

    /**
     * Recompute participation rate whenever voter count or vote count changes.
     * Cancels any in-flight recompute to prevent race conditions when both
     * elections and users observers fire in quick succession.
     */
    private fun recomputeStats() {
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch {
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
    }

    // ==================== DATA LOADING ====================

    /**
     * Load all data via real-time observers. Called from screen composable
     * via LaunchedEffect (first composition) and from "Coba Lagi" / refresh buttons.
     * Safe to call multiple times — starts observers on first call, skips subsequent calls.
     */
    fun loadAllData() {
        val userId = sessionManager.getUserId()
        if (userId == null) {
            _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid. Silakan login ulang.")
            return
        }

        if (!observing) {
            observing = true
            startRealTimeObservers()
        } else {
            // Force-reload: cancel and restart all observers for a fresh snapshot
            electionsJob?.cancel()
            usersJob?.cancel()
            securityJob?.cancel()
            observing = false
            startRealTimeObservers()
            observing = true
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
                    // Real-time observer auto-picks up the new election
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
            when (val r = authRepo.updateUserRole(uid, newRole)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.USER_ACTIVATED, uid, "Role: $newRole")
                    _state.value = _state.value.copy(successMessage = "Role pengguna diperbarui")
                    // Real-time observer auto-picks up the role change
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    fun updateUserStatus(uid: String, status: UserStatus) {
        viewModelScope.launch {
            when (val r = authRepo.updateUserStatus(uid, status)) {
                is Resource.Success -> {
                    val action = if (status == UserStatus.SUSPENDED) AuditAction.USER_SUSPENDED else AuditAction.USER_ACTIVATED
                    auditRepository.log(userId, userName, userRole, action, uid)
                    _state.value = _state.value.copy(successMessage = "Status pengguna diperbarui")
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    // ==================== REPORT ====================

    /**
     * Quick report preview from cached data. Use [generateReportText] for exports.
     */
    fun getElectionReportPreview(electionId: String): String {
        val election = _state.value.elections.find { it.id == electionId } ?: return "Pemilihan tidak ditemukan"
        val candidates = _state.value.candidates.ifEmpty {
            return "Muat kandidat terlebih dahulu — pilih pemilihan di menu Kandidat"
        }
        return buildReportString(election, candidates)
    }

    private fun buildReportString(election: Election, candidates: List<Candidate>): String {
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

    /**
     * Generate an accurate election report by loading FRESH candidate data
     * with current vote counts directly from Firestore.
     */
    suspend fun generateReportText(electionId: String): String {
        val election = _state.value.elections.find { it.id == electionId }
            ?: return "Pemilihan tidak ditemukan"
        val result = candidateRepository.getCandidates(electionId)
        val candidates = result.getOrNull()
            ?: return result.errorOrNull() ?: "Gagal memuat data kandidat"
        return buildReportString(election, candidates)
    }

    /** Export report as plain text — loads fresh data from Firestore. */
    suspend fun exportReport(electionId: String): String = generateReportText(electionId)

    /**
     * Generate a PDF report of the election results using Android's PdfDocument API.
     * Loads fresh candidate data with current vote counts.
     * Saves to app cache directory and returns the file path, or null on failure.
     */
    suspend fun generatePdfReport(context: Context, electionId: String): String? {
        val reportText = generateReportText(electionId)
        if (reportText.isBlank()) return null

        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 20f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 12f
            }

            var y = 40f
            val margin = 40f
            val pageWidth = 595f
            val lineHeight = 18f

            // Draw title
            canvas.drawText("Laporan Pemilihan", pageWidth / 2f - 60f, y, titlePaint)
            y += lineHeight * 2

            // Draw body text with line wrapping
            val lines = reportText.lines()
            for (line in lines) {
                if (line.isBlank()) {
                    y += lineHeight * 0.5f
                    continue
                }
                // Simple word wrap for long lines
                val words = line.split(" ")
                val sb = StringBuilder()
                for (word in words) {
                    val testLine = if (sb.isEmpty()) word else "$sb $word"
                    if (bodyPaint.measureText(testLine) > pageWidth - margin * 2) {
                        canvas.drawText(sb.toString(), margin, y, bodyPaint)
                        y += lineHeight
                        sb.clear()
                    }
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(word)
                }
                if (sb.isNotEmpty()) {
                    canvas.drawText(sb.toString(), margin, y, bodyPaint)
                    y += lineHeight
                }
            }

            document.finishPage(page)

            // Save to cache directory
            val fileName = "report_${electionId}_${System.currentTimeMillis()}.pdf"
            val file = java.io.File(context.cacheDir, fileName)
            document.writeTo(java.io.FileOutputStream(file))
            document.close()

            Log.d(TAG, "PDF report saved to: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate PDF report", e)
            null
        }
    }

    // ==================== ANNOUNCEMENT MANAGEMENT ====================

    fun loadAnnouncements() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAnnouncementLoading = true)
            when (val r = announcementRepository.getAllAnnouncements()) {
                is Resource.Success -> _state.value = _state.value.copy(
                    announcements = r.data,
                    isAnnouncementLoading = false
                )
                is Resource.Error -> _state.value = _state.value.copy(
                    isAnnouncementLoading = false,
                    error = r.message
                )
                else -> _state.value = _state.value.copy(isAnnouncementLoading = false)
            }
        }
    }

    fun createAnnouncement(title: String, message: String, priority: String) {
        viewModelScope.launch {
            val safeTitle = InputSanitizer.sanitizeText(title, 300)
            val safeMessage = InputSanitizer.sanitizeText(message, 2000)
            if (safeTitle.isBlank()) {
                _state.value = _state.value.copy(error = "Judul pengumuman tidak valid")
                return@launch
            }
            _state.value = _state.value.copy(isLoading = true)
            val announcement = Announcement(
                title = safeTitle,
                message = safeMessage,
                priority = priority,
                createdBy = userId,
                createdAt = System.currentTimeMillis()
            )
            when (val r = announcementRepository.createAnnouncement(announcement)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.ANNOUNCEMENT_CREATED, r.data.id, title)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "Pengumuman berhasil dibuat"
                    )
                    loadAnnouncements()
                }
                is Resource.Error -> _state.value = _state.value.copy(isLoading = false, error = r.message)
                else -> {}
            }
        }
    }

    fun updateAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            val safeTitle = InputSanitizer.sanitizeText(announcement.title, 300)
            val safeMessage = InputSanitizer.sanitizeText(announcement.message, 2000)
            _state.value = _state.value.copy(isLoading = true)
            val updated = announcement.copy(title = safeTitle, message = safeMessage)
            when (val r = announcementRepository.updateAnnouncement(updated)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.ANNOUNCEMENT_UPDATED, r.data.id, safeTitle)
                    _state.value = _state.value.copy(isLoading = false, successMessage = "Pengumuman diperbarui")
                    loadAnnouncements()
                }
                is Resource.Error -> _state.value = _state.value.copy(isLoading = false, error = r.message)
                else -> {}
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            when (val r = announcementRepository.deleteAnnouncement(announcementId)) {
                is Resource.Success -> {
                    auditRepository.log(userId, userName, userRole, AuditAction.ANNOUNCEMENT_DELETED, announcementId)
                    _state.value = _state.value.copy(successMessage = "Pengumuman dihapus")
                    loadAnnouncements()
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    fun toggleAnnouncementActive(announcement: Announcement) {
        viewModelScope.launch {
            val updated = announcement.copy(isActive = !announcement.isActive)
            when (val r = announcementRepository.updateAnnouncement(updated)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(successMessage = "Status pengumuman diperbarui")
                    loadAnnouncements()
                }
                is Resource.Error -> _state.value = _state.value.copy(error = r.message)
                else -> {}
            }
        }
    }

    // ==================== UTILITY ====================

    fun clearMessages() { _state.value = _state.value.copy(error = null, successMessage = null) }
}
