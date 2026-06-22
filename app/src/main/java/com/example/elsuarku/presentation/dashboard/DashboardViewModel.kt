package com.example.elsuarku.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.repository.AuthRepository
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.VoteRepository
import com.example.elsuarku.security.IntegrityVerifier
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val electionRepository: ElectionRepository,
    private val voteRepository: VoteRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    data class UserDashboardState(
        val activeElections: List<Election> = emptyList(),
        val allElections: List<Election> = emptyList(),
        val votedElectionIds: Set<String> = emptySet(),
        val userName: String = "",
        val userEmail: String = "",
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UserDashboardState())
    val state: StateFlow<UserDashboardState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(
            userName = sessionManager.getUserName(),
            userEmail = sessionManager.getUserEmail()
        )
        loadDashboard()
    }

    fun loadDashboard(forceReload: Boolean = false) {
        val shouldLoadElections = forceReload || _state.value.activeElections.isEmpty()

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            if (shouldLoadElections) {
                // Load active elections
                when (val result = electionRepository.getActiveElections()) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(activeElections = result.data)
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                        return@launch
                    }
                    is Resource.Loading -> {}
                }
            }

            // Always refresh voted election IDs — voting status can change between visits
            checkVotedElections(_state.value.activeElections)
        }
    }

    /**
     * Check which elections the user has voted in.
     * Uses anonymous voterHash (SHA-256 of userId:electionId) — no plaintext PII in queries.
     */
    private suspend fun checkVotedElections(activeElections: List<Election>) {
        val userId = sessionManager.getUserId()
        if (userId == null) {
            _state.value = _state.value.copy(isLoading = false, error = "Sesi tidak valid. Silakan login ulang.")
            return
        }
        val votedIds = mutableSetOf<String>()

        // Check active elections using anonymous voterHash
        for (election in activeElections) {
            val voterHash = IntegrityVerifier.computeVoterHash(userId, election.id)
            when (val result = voteRepository.checkUserHasVoted(voterHash, election.id)) {
                is Resource.Success -> { if (result.data) votedIds.add(election.id) }
                else -> {}
            }
        }

        // Also load ALL elections to catch votes in completed elections
        var allElections: List<Election> = emptyList()
        when (val allResult = electionRepository.getAllElections()) {
            is Resource.Success -> {
                allElections = allResult.data
                for (election in allResult.data) {
                    if (election.id !in activeElections.map { it.id }) {
                        val voterHash = IntegrityVerifier.computeVoterHash(userId, election.id)
                        when (val result = voteRepository.checkUserHasVoted(voterHash, election.id)) {
                            is Resource.Success -> { if (result.data) votedIds.add(election.id) }
                            else -> {}
                        }
                    }
                }
            }
            else -> {}
        }

        _state.value = _state.value.copy(
            votedElectionIds = votedIds,
            allElections = allElections,
            isLoading = false
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
