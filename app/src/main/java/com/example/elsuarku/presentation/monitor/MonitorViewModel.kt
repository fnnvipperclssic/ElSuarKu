package com.example.elsuarku.presentation.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.elsuarku.data.model.AuditLog
import com.example.elsuarku.data.model.AuditSeverity
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.data.repository.AuthRepository
import com.example.elsuarku.data.repository.CandidateRepository
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.VoteRepository
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MonitorViewModel(
    private val electionRepository: ElectionRepository,
    private val voteRepository: VoteRepository,
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository,
    private val candidateRepository: CandidateRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    data class MonitorState(
        val elections: List<Election> = emptyList(),
        val selectedElection: Election? = null,
        val electionVoteCount: Int = 0,
        val totalVotes: Int = 0,
        val totalUsers: Int = 0,
        val monitorName: String = "",
        val auditLogs: List<AuditLog> = emptyList(),
        val securityAlerts: List<AuditLog> = emptyList(),
        val candidates: List<Candidate> = emptyList(),
        val criticalAlertCount: Int = 0,
        val warningAlertCount: Int = 0,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(MonitorState())
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    companion object { private const val TAG = "MonitorViewModel" }

    init {
        _state.value = _state.value.copy(monitorName = sessionManager.getUserName())
        viewModelScope.launch { loadMonitorDataInternal() }
        observeAuditLogs()
    }

    /** Suspend body of monitor data load — reusable by init and refresh(). */
    private suspend fun loadMonitorDataInternal() {
        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            candidates = emptyList(),     // clear stale per-election data
            selectedElection = null,
            electionVoteCount = 0
        )

        // Parallelize independent queries (coroutineScope needed for async in suspend fun)
        val errors = mutableListOf<String>()
        coroutineScope {
            val electionsDeferred = async { electionRepository.getActiveElections() }
            val votesDeferred = async { voteRepository.getTotalVotesCount() }
            val usersDeferred = async { authRepository.getAllUsers() }

            when (val er = electionsDeferred.await()) {
                is Resource.Success -> _state.value = _state.value.copy(elections = er.data)
                is Resource.Error -> errors.add("Pemilihan: ${er.message}")
                else -> {}
            }

            when (val vr = votesDeferred.await()) {
                is Resource.Success -> _state.value = _state.value.copy(totalVotes = vr.data)
                is Resource.Error -> errors.add("Suara: ${vr.message}")
                else -> {}
            }

            when (val ur = usersDeferred.await()) {
                is Resource.Success -> _state.value = _state.value.copy(totalUsers = ur.data.size)
                is Resource.Error -> errors.add("Pengguna: ${ur.message}")
                else -> {}
            }
        }

        loadSecurityAlerts()

        _state.value = _state.value.copy(
            isLoading = false,
            error = if (errors.isNotEmpty()) errors.joinToString("\n") else null
        )
    }

    private fun observeAuditLogs() {
        viewModelScope.launch {
            auditRepository.observeRecentLogs(100).collect { result ->
                when (result) {
                    is Resource.Success -> _state.value = _state.value.copy(auditLogs = result.data)
                    is Resource.Error -> Log.w(TAG, "Audit log stream error: ${result.message}")
                    else -> {}
                }
            }
        }
    }

    fun loadElectionStats(electionId: String) {
        viewModelScope.launch { loadElectionStatsInternal(electionId) }
    }

    private suspend fun loadElectionStatsInternal(electionId: String) {
        // Clear previous election-specific data before loading new
        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            selectedElection = null,
            electionVoteCount = 0,
            candidates = emptyList()
        )
        val errors = mutableListOf<String>()

        // Load election detail
        when (val result = electionRepository.getElection(electionId)) {
            is Resource.Success -> _state.value = _state.value.copy(selectedElection = result.data)
            is Resource.Error -> errors.add(result.message)
            else -> {}
        }

        // Load vote count
        when (val result = voteRepository.getVoteCountForElection(electionId)) {
            is Resource.Success -> _state.value = _state.value.copy(electionVoteCount = result.data)
            is Resource.Error -> errors.add(result.message)
            else -> {}
        }

        // Load candidates with vote counts for this election
        when (val result = candidateRepository.getCandidates(electionId)) {
            is Resource.Success -> _state.value = _state.value.copy(candidates = result.data)
            is Resource.Error -> errors.add(result.message)
            else -> {}
        }

        _state.value = _state.value.copy(
            isLoading = false,
            error = if (errors.isNotEmpty()) errors.joinToString("\n") else null
        )
    }

    private suspend fun loadSecurityAlerts() {
        var critical: List<AuditLog> = emptyList()
        var warnings: List<AuditLog> = emptyList()
        var info: List<AuditLog> = emptyList()
        val alertErrors = mutableListOf<String>()

        // Load all severities independently — one failure doesn't block the others
        when (val result = auditRepository.getLogsBySeverity(AuditSeverity.CRITICAL, 50)) {
            is Resource.Success -> critical = result.data
            is Resource.Error -> {
                Log.w(TAG, "Gagal memuat log CRITICAL: ${result.message}")
                alertErrors.add("Critical: ${result.message}")
            }
            else -> {}
        }

        when (val result = auditRepository.getLogsBySeverity(AuditSeverity.WARNING, 50)) {
            is Resource.Success -> warnings = result.data
            is Resource.Error -> {
                Log.w(TAG, "Gagal memuat log WARNING: ${result.message}")
                alertErrors.add("Warning: ${result.message}")
            }
            else -> {}
        }

        when (val result = auditRepository.getLogsBySeverity(AuditSeverity.INFO, 50)) {
            is Resource.Success -> info = result.data
            is Resource.Error -> {
                Log.w(TAG, "Gagal memuat log INFO: ${result.message}")
                alertErrors.add("Info: ${result.message}")
            }
            else -> {}
        }

        _state.value = _state.value.copy(
            securityAlerts = critical + warnings + info,
            criticalAlertCount = critical.size,
            warningAlertCount = warnings.size,
            // Propagate security-specific errors so screens don't show false "Sistem AMAN"
            error = if (alertErrors.isNotEmpty() && _state.value.error == null)
                "Gagal memuat peringatan keamanan: ${alertErrors.joinToString("; ")}"
            else _state.value.error
        )
    }

    fun refresh() {
        viewModelScope.launch {
            loadMonitorDataInternal()
            // Sequentially load election stats after monitor data completes —
            // eliminates the race between two concurrent state-mutating coroutines
            _state.value.selectedElection?.let { election ->
                loadElectionStatsInternal(election.id)
            }
        }
    }
}
