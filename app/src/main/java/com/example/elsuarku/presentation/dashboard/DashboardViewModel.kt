package com.example.elsuarku.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.VoteRepository
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val electionRepository: ElectionRepository,
    private val voteRepository: VoteRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    data class UserDashboardState(
        val activeElections: List<Election> = emptyList(),
        val votedElectionIds: Set<String> = emptySet(),
        val userName: String = "",
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UserDashboardState())
    val state: StateFlow<UserDashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Load active elections
            when (val result = electionRepository.getActiveElections()) {
                is Resource.Success -> {
                    val elections = result.data
                    _state.value = _state.value.copy(
                        activeElections = elections,
                        isLoading = false
                    )
                    // Check which elections user has voted in
                    checkVotedElections(elections)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    private suspend fun checkVotedElections(elections: List<Election>) {
        val userId = sessionManager.getUserId() ?: return
        val votedIds = mutableSetOf<String>()

        for (election in elections) {
            when (val result = voteRepository.checkUserHasVoted(userId, election.id)) {
                is Resource.Success -> {
                    if (result.data) votedIds.add(election.id)
                }
                else -> {}
            }
        }

        _state.value = _state.value.copy(votedElectionIds = votedIds)
    }

    fun refreshSession() {
        sessionManager.refreshSession()
    }
}
