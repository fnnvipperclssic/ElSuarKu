package com.example.elsuarku.presentation.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.VoteRepository
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MonitorViewModel(
    private val electionRepository: ElectionRepository,
    private val voteRepository: VoteRepository,
    private val auditRepository: AuditRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    data class MonitorState(
        val elections: List<Election> = emptyList(),
        val selectedElection: Election? = null,
        val electionVoteCount: Int = 0,
        val totalVotes: Int = 0,
        val auditLogs: List<AuditLog> = emptyList(),
        val securityAlerts: List<AuditLog> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(MonitorState())
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    init {
        loadMonitorData()
        observeAuditLogs()
    }

    private fun loadMonitorData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Load all elections
            when (val result = electionRepository.getActiveElections()) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(elections = result.data)
                }
                else -> {}
            }

            // Load total votes
            when (val result = voteRepository.getTotalVotesCount()) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(totalVotes = result.data)
                }
                else -> {}
            }

            // Load security alerts (critical + warning)
            loadSecurityAlerts()

            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private fun observeAuditLogs() {
        viewModelScope.launch {
            auditRepository.observeRecentLogs(100).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(auditLogs = result.data)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadElectionStats(electionId: String) {
        viewModelScope.launch {
            // Load election detail
            when (val result = electionRepository.getElection(electionId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(selectedElection = result.data)
                }
                else -> {}
            }
            // Load vote count
            when (val result = voteRepository.getVoteCountForElection(electionId)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(electionVoteCount = result.data)
                }
                else -> {}
            }
        }
    }

    private suspend fun loadSecurityAlerts() {
        when (val result = auditRepository.getLogsBySeverity(AuditSeverity.CRITICAL, 50)) {
            is Resource.Success -> {
                val critical = result.data
                val warnings = when (val wr = auditRepository.getLogsBySeverity(AuditSeverity.WARNING, 50)) {
                    is Resource.Success -> wr.data
                    else -> emptyList()
                }
                _state.value = _state.value.copy(securityAlerts = critical + warnings)
            }
            else -> {}
        }
    }

    fun refresh() {
        loadMonitorData()
        viewModelScope.launch {
            loadSecurityAlerts()
        }
    }
}
