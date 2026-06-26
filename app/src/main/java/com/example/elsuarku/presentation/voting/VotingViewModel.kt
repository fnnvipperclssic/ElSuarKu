package com.example.elsuarku.presentation.voting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.domain.repository.IAuditRepository
import com.example.elsuarku.domain.repository.ICandidateRepository
import com.example.elsuarku.domain.repository.IElectionRepository
import com.example.elsuarku.domain.repository.IVoteRepository
import com.example.elsuarku.domain.usecase.SubmitVoteParams
import com.example.elsuarku.domain.usecase.SubmitVoteResult
import com.example.elsuarku.domain.usecase.SubmitVoteUseCase
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
    private val auditRepository: IAuditRepository,
    private val submitVoteUseCase: SubmitVoteUseCase
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

            // Compute anonymous voter hash
            val voterHash = IntegrityVerifier.computeVoterHash(userId, electionId)

            // Encrypt vote + HMAC integrity + verification token
            val encryptedData: String; val hash: String; val hmac: String; val verificationToken: String
            try {
                val timestamp = System.currentTimeMillis()
                val voteData = "vote:$userId:$electionId:$candidateId:$timestamp"
                encryptedData = encryptionManager.encrypt(voteData)
                val integrity = IntegrityVerifier.generateVoteSignature(
                    userId, electionId, candidateId, timestamp
                )
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

            // Delegate to use case for two-phase commit submission
            val params = SubmitVoteParams(
                userId = userId,
                userName = sessionManager.getUserName(),
                userRole = sessionManager.getUserRole()?.name ?: "",
                electionId = electionId,
                candidateId = candidateId,
                voterHash = voterHash,
                encryptedVoteData = encryptedData,
                hash = hash,
                hmac = hmac,
                verificationToken = verificationToken
            )

            when (val result = submitVoteUseCase.execute(params, _voteState.value.election)) {
                is SubmitVoteResult.Success -> {
                    // Clear step-up auth — needs fresh auth for next vote
                    sessionManager.clearStepUpAuth()

                    Log.i(TAG, "submitVote: SUCCESS! Vote cast securely.")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        verificationToken = result.verificationToken
                    )
                }
                is SubmitVoteResult.Error -> {
                    Log.e(TAG, "submitVote: FAILED — ${result.error.technical}")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = result.error.userMessage
                    )
                }
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
