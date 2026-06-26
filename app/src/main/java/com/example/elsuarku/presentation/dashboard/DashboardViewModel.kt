package com.example.elsuarku.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.Announcement
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.domain.repository.IAnnouncementRepository
import com.example.elsuarku.domain.repository.IAuthRepository
import com.example.elsuarku.domain.repository.IElectionRepository
import com.example.elsuarku.domain.repository.IVoteRepository
import com.example.elsuarku.security.IntegrityVerifier
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(
    private val electionRepository: IElectionRepository,
    private val voteRepository: IVoteRepository,
    private val authRepository: IAuthRepository,
    private val sessionManager: SessionManager,
    private val announcementRepository: IAnnouncementRepository
) : ViewModel() {

    data class UserDashboardState(
        val activeElections: List<Election> = emptyList(),
        val allElections: List<Election> = emptyList(),
        val votedElectionIds: Set<String> = emptySet(),
        val userName: String = "",
        val userEmail: String = "",
        val announcements: List<Announcement> = emptyList(),
        val dismissedAnnouncementIds: Set<String> = emptySet(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    companion object { private const val TAG = "DashboardVM" }

    private val _state = MutableStateFlow(UserDashboardState())
    val state: StateFlow<UserDashboardState> = _state.asStateFlow()

    private var voteStatusJob: Job? = null
    private var historyLoaded = false
    private var observing = false

    init {
        val uid = sessionManager.getUserId()
        val name = sessionManager.getUserName()
        android.util.Log.d(TAG, "init: userId=$uid name=$name")

        if (uid != null) {
            // User is logged in — show name/email while data loads
            _state.value = _state.value.copy(
                userName = name,
                userEmail = sessionManager.getUserEmail(),
                isLoading = true  // stay loading until loadDashboard() resolves
            )
        } else {
            // No active session — keep loading, let loadDashboard() surface the error
            _state.value = _state.value.copy(
                userName = "",
                userEmail = "",
                isLoading = true
            )
        }
    }

    /**
     * Start real-time observers if not already running.
     * Called from screen composable via LaunchedEffect.
     */
    fun loadDashboard(forceReload: Boolean = false) {
        val userId = sessionManager.getUserId()
        android.util.Log.d(TAG, "loadDashboard: userId=$userId forceReload=$forceReload observing=$observing")
        if (userId == null) {
            android.util.Log.w(TAG, "loadDashboard: userId is NULL — session invalid!")
            _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid. Silakan login ulang.")
            return
        }

        if (observing && !forceReload) {
            android.util.Log.d(TAG, "loadDashboard: already observing, skip")
            return
        }
        observing = true

        _state.value = _state.value.copy(isLoading = true, error = null)
        android.util.Log.d(TAG, "loadDashboard: starting real-time observers")

        // ── Real-time: observe active elections via Firestore snapshot listener ──
        viewModelScope.launch {
            electionRepository.observeActiveElections().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val elections = result.data
                        _state.value = _state.value.copy(
                            activeElections = elections,
                            isLoading = false,
                            error = null
                        )
                        observeVoteStatus(userId, elections)

                        if (!historyLoaded) {
                            historyLoaded = true
                            loadAllElectionsHistory(userId)
                        }
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    is Resource.Loading -> { /* no-op */ }
                    is Resource.Empty -> { /* no-op */ }
                    is Resource.Cached -> {
                        _state.value = _state.value.copy(
                            activeElections = result.data,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Real-time observer for vote status across the given elections.
     * Cancels the previous observation and starts a new one scoped to the
     * current list of election IDs.
     */
    private fun observeVoteStatus(userId: String, elections: List<Election>) {
        voteStatusJob?.cancel()
        if (elections.isEmpty()) return
        val electionIds = elections.map { it.id }
        voteStatusJob = viewModelScope.launch {
            voteRepository
                .observeUserVoteStatus(userId, electionIds)
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            // Merge real-time voted IDs with any history-only voted IDs
                            val historyVoted = _state.value.votedElectionIds
                                .filter { it !in electionIds }
                                .toSet()
                            _state.value = _state.value.copy(
                                votedElectionIds = result.data + historyVoted
                            )
                        }
                        else -> { /* keep stale data on error */ }
                    }
                }
        }
    }

    /**
     * One-shot load of ALL elections (including completed/cancelled) for voting history.
     * Uses parallel coroutines with Dispatchers.IO for batch vote-status checking.
     */
    private suspend fun loadAllElectionsHistory(userId: String) {
        when (val allResult = electionRepository.getAllElections()) {
            is Resource.Success -> {
                val allElections = allResult.data
                val activeIds = _state.value.activeElections.map { it.id }.toSet()
                val historyElections = allElections.filter { it.id !in activeIds }

                // Check voted status for elections NOT in active list — parallel IO
                val historyVotedIds = if (historyElections.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        historyElections.map { election ->
                            async {
                                val voterHash = IntegrityVerifier.computeVoterHash(userId, election.id)
                                when (val vr = voteRepository.checkUserHasVoted(voterHash, election.id)) {
                                    is Resource.Success -> if (vr.data) election.id else null
                                    else -> null
                                }
                            }
                        }.mapNotNull { it.await() }.toSet()
                    }
                } else emptySet()

                _state.value = _state.value.copy(
                    allElections = allElections,
                    votedElectionIds = _state.value.votedElectionIds + historyVotedIds
                )
            }
            else -> { /* keep whatever we have */ }
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            when (val r = announcementRepository.getActiveAnnouncements()) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(announcements = r.data)
                }
                else -> { /* keep whatever we have */ }
            }
        }
    }

    fun dismissAnnouncement(announcementId: String) {
        _state.value = _state.value.copy(
            dismissedAnnouncementIds = _state.value.dismissedAnnouncementIds + announcementId
        )
    }

    fun refreshSession() {
        sessionManager.refreshSession()
    }

    /**
     * Load user's profile photo from Firestore.
     * Returns Base64 string or null if no photo set.
     */
    suspend fun loadUserPhoto(userId: String): String? {
        return authRepository.getUserPhoto(userId)
    }

    /**
     * Update user's profile photo in Firestore.
     * Returns true on success.
     */
    suspend fun updateProfilePhoto(userId: String, photoBase64: String): Boolean {
        return authRepository.updateProfilePhoto(userId, photoBase64)
    }
}
