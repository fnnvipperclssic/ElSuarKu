package com.example.elsuarku.presentation.voting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.data.model.ReconciliationStatus
import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.domain.repository.IAuditRepository
import com.example.elsuarku.domain.repository.ICandidateRepository
import com.example.elsuarku.domain.repository.IElectionRepository
import com.example.elsuarku.domain.repository.IVoteRepository
import com.example.elsuarku.security.EncryptionManager
import com.example.elsuarku.security.IntegrityVerifier
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch

class VotingViewModel(
    private val electionRepository: IElectionRepository,
    private val candidateRepository: ICandidateRepository,
    private val voteRepository: IVoteRepository,
    private val encryptionManager: EncryptionManager,
    private val sessionManager: SessionManager,
    private val auditRepository: IAuditRepository
) : ViewModel() {

    data class ElectionListState(
        val elections: List<Election> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    data class CandidateListState(
        val election: Election? = null,
        val candidates: List<Candidate> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    data class VoteState(
        val candidate: Candidate? = null,
        val election: Election? = null,
        val isSubmitting: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null,
        val verificationToken: String? = null,
        val biometricVerified: Boolean = false
    )

    private val _electionListState = MutableStateFlow(ElectionListState())
    val electionListState: StateFlow<ElectionListState> = _electionListState.asStateFlow()

    private val _candidateListState = MutableStateFlow(CandidateListState())
    val candidateListState: StateFlow<CandidateListState> = _candidateListState.asStateFlow()

    private val _voteState = MutableStateFlow(VoteState())
    val voteState: StateFlow<VoteState> = _voteState.asStateFlow()

    companion object { private const val TAG = "VotingViewModel" }

    private var electionsJob: Job? = null
    private var candidatesJob: Job? = null

    /**
     * Start observing active elections in real-time via Firestore snapshot listener.
     * Idempotent — safe to call multiple times (e.g. from LaunchedEffect on recomposition).
     */
    fun loadElections() {
        if (electionsJob?.isActive == true) return
        electionsJob = viewModelScope.launch {
            electionRepository.observeActiveElections().cancellable().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _electionListState.value = ElectionListState(
                            elections = result.data,
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _electionListState.value = ElectionListState(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    is Resource.Loading -> {}
                    is Resource.Empty -> {}
                    is Resource.Cached -> {}
                }
            }
        }
    }

    /**
     * Observe candidates for a specific election in real-time.
     * Cancels any previous candidate observation before starting a new one.
     */
    fun loadCandidates(electionId: String) {
        candidatesJob?.cancel()
        candidatesJob = viewModelScope.launch {
            // Election details (one-shot — election metadata rarely changes)
            when (val electionResult = electionRepository.getElection(electionId)) {
                is Resource.Success -> {
                    _candidateListState.value = _candidateListState.value.copy(
                        election = electionResult.data
                    )
                }
                else -> {}
            }

            // Real-time candidate list via Firestore snapshot listener
            candidateRepository.observeCandidates(electionId).cancellable().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _candidateListState.value = CandidateListState(
                            election = _candidateListState.value.election,
                            candidates = result.data,
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _candidateListState.value = CandidateListState(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    is Resource.Loading -> {}
                    is Resource.Empty -> {}
                    is Resource.Cached -> {}
                }
            }
        }
    }

    fun loadCandidateForVote(electionId: String, candidateId: String) {
        viewModelScope.launch {
            _voteState.value = VoteState(isSubmitting = false)
            // Load election
            when (val er = electionRepository.getElection(electionId)) {
                is Resource.Success -> _voteState.value = _voteState.value.copy(election = er.data)
                else -> {}
            }
            // Load candidate
            when (val cr = candidateRepository.getCandidate(candidateId)) {
                is Resource.Success -> _voteState.value = _voteState.value.copy(candidate = cr.data)
                else -> {}
            }
        }
    }

    fun submitVote(electionId: String, candidateId: String) {
        viewModelScope.launch {
            Log.i(TAG, "submitVote: starting — election=$electionId candidate=$candidateId")
            _voteState.value = _voteState.value.copy(isSubmitting = true, error = null)

            val userId = sessionManager.getUserId() ?: run {
                Log.e(TAG, "submitVote: FAILED — no userId in session")
                _voteState.value = _voteState.value.copy(
                    isSubmitting = false,
                    error = "Sesi tidak valid. Silakan login ulang."
                )
                return@launch
            }
            Log.d(TAG, "submitVote: userId=$userId")

            // ── S4: Validate election is still active and not expired ──
            val election = _voteState.value.election
            if (election != null) {
                if (election.status != ElectionStatus.ACTIVE) {
                    Log.w(TAG, "submitVote: REJECTED — election status=${election.status}")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = "Pemilihan sudah tidak aktif (status: ${election.status.name})"
                    )
                    return@launch
                }
                if (System.currentTimeMillis() > election.endDate) {
                    Log.w(TAG, "submitVote: REJECTED — election expired")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = "Pemilihan sudah berakhir"
                    )
                    return@launch
                }
                if (System.currentTimeMillis() < election.startDate) {
                    Log.w(TAG, "submitVote: REJECTED — election not yet started")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = "Pemilihan belum dimulai"
                    )
                    return@launch
                }
            }
            Log.d(TAG, "submitVote: election validation OK")

            // Compute anonymous voter hash
            val voterHash = IntegrityVerifier.computeVoterHash(userId, electionId)
            Log.d(TAG, "submitVote: voterHash=${voterHash.take(8)}...")

            // Encrypt vote + HMAC integrity + verification token
            val encryptedData: String
            val hash: String
            val hmac: String
            val verificationToken: String
            try {
                val timestamp = System.currentTimeMillis()
                val voteData = "vote:$userId:$electionId:$candidateId:$timestamp"
                encryptedData = encryptionManager.encrypt(voteData)
                val integrity = IntegrityVerifier.generateVoteSignature(userId, electionId, candidateId, timestamp)
                hash = integrity.hash
                hmac = integrity.hmac
                verificationToken = integrity.verificationToken
            } catch (e: Exception) {
                Log.e(TAG, "submitVote: encryption/integrity FAILED", e)
                _voteState.value = _voteState.value.copy(
                    isSubmitting = false,
                    error = "Gagal mengamankan suara: ${e.localizedMessage ?: "kesalahan enkripsi"}"
                )
                return@launch
            }

            val vote = Vote(
                electionId = electionId,
                voterHash = voterHash,
                encryptedVoteData = encryptedData,
                hash = hash,
                hmac = hmac,
                verificationToken = verificationToken,
                reconciliationStatus = ReconciliationStatus.PENDING_RECONCILIATION
            )

            // ── TWO-PHASE COMMIT ──
            // Phase 1: Atomically write the vote document (prevents double-voting)
            Log.d(TAG, "submitVote: PHASE 1 — atomic vote write via transaction...")
            when (val result = voteRepository.submitVote(vote)) {
                is Resource.Success -> {
                    Log.i(TAG, "submitVote: PHASE 1 OK — vote persisted")

                    // Phase 2a: Increment candidate counter (critical)
                    val candidateOk = try {
                        candidateRepository.incrementVoteCount(candidateId)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "submitVote: PHASE 2a FAILED — candidate counter", e)
                        false
                    }

                    // Phase 2b: Increment election counter (critical)
                    val electionOk = try {
                        electionRepository.incrementVotedCount(electionId)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "submitVote: PHASE 2b FAILED — election counter", e)
                        false
                    }

                    // If both counters succeeded, mark vote CONFIRMED
                    if (candidateOk && electionOk) {
                        Log.i(TAG, "submitVote: PHASE 2 OK — all counters synced")
                        // Update reconciliation status to CONFIRMED
                        // (fire-and-forget — vote is already safe)
                        try {
                            voteRepository.updateReconciliationStatus(
                                voterHash, electionId,
                                ReconciliationStatus.CONFIRMED
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to update reconciliation status", e)
                        }
                    } else {
                        // Vote is saved but counters inconsistent — needs reconciliation
                        Log.w(TAG, "submitVote: PHASE 2 PARTIAL — vote saved, counters need reconciliation")
                    }

                    // Phase 3: Audit log (non-critical, fire-and-forget)
                    try {
                        auditRepository.log(
                            actorId = userId,
                            actorName = sessionManager.getUserName(),
                            actorRole = sessionManager.getUserRole()?.name ?: "",
                            action = AuditAction.VOTE_CAST,
                            target = electionId,
                            targetName = candidateId,
                            detail = "reconciliation=${if (candidateOk && electionOk) "CONFIRMED" else "PENDING"}"
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "audit log failed (non-critical)", e)
                    }

                    // Clear step-up auth — needs fresh auth for next vote
                    sessionManager.clearStepUpAuth()

                    Log.i(TAG, "submitVote: SUCCESS! Vote cast securely.")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        verificationToken = verificationToken
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "submitVote: PHASE 1 FAILED — ${result.message}")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {}
                is Resource.Empty -> {}
                is Resource.Cached -> {}
            }
        }
    }

    fun resetVoteState() {
        _voteState.value = VoteState()
    }

    fun setBiometricVerified() {
        _voteState.value = _voteState.value.copy(biometricVerified = true)
    }

    fun clearVoteError() {
        _voteState.value = _voteState.value.copy(error = null)
    }
}
