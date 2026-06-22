package com.example.elsuarku.presentation.voting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.data.model.Vote
import com.example.elsuarku.data.repository.AuditRepository
import com.example.elsuarku.data.repository.CandidateRepository
import com.example.elsuarku.data.repository.ElectionRepository
import com.example.elsuarku.data.repository.VoteRepository
import com.example.elsuarku.security.EncryptionManager
import com.example.elsuarku.security.IntegrityVerifier
import com.example.elsuarku.security.SessionManager
import com.example.elsuarku.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VotingViewModel(
    private val electionRepository: ElectionRepository,
    private val candidateRepository: CandidateRepository,
    private val voteRepository: VoteRepository,
    private val encryptionManager: EncryptionManager,
    private val sessionManager: SessionManager,
    private val auditRepository: AuditRepository
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
        val verificationToken: String? = null
    )

    private val _electionListState = MutableStateFlow(ElectionListState())
    val electionListState: StateFlow<ElectionListState> = _electionListState.asStateFlow()

    private val _candidateListState = MutableStateFlow(CandidateListState())
    val candidateListState: StateFlow<CandidateListState> = _candidateListState.asStateFlow()

    private val _voteState = MutableStateFlow(VoteState())
    val voteState: StateFlow<VoteState> = _voteState.asStateFlow()

    companion object { private const val TAG = "VotingViewModel" }

    fun loadElections() {
        viewModelScope.launch {
            _electionListState.value = ElectionListState(isLoading = true)
            when (val result = electionRepository.getActiveElections()) {
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
            }
        }
    }

    fun loadCandidates(electionId: String) {
        viewModelScope.launch {
            _candidateListState.value = CandidateListState(isLoading = true)
            // Load election details
            when (val electionResult = electionRepository.getElection(electionId)) {
                is Resource.Success -> {
                    _candidateListState.value = _candidateListState.value.copy(
                        election = electionResult.data
                    )
                }
                else -> {}
            }
            // Load candidates
            when (val result = candidateRepository.getCandidates(electionId)) {
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
                    Log.w(TAG, "submitVote: REJECTED — election expired (endDate=${election.endDate})")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = "Pemilihan sudah berakhir"
                    )
                    return@launch
                }
                if (System.currentTimeMillis() < election.startDate) {
                    Log.w(TAG, "submitVote: REJECTED — election not yet started (startDate=${election.startDate})")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = "Pemilihan belum dimulai"
                    )
                    return@launch
                }
            }
            Log.d(TAG, "submitVote: election validation OK")

            // Compute anonymous voter hash (S2: no plaintext userId in Firestore)
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
                Log.d(TAG, "submitVote: encrypting vote data...")
                encryptedData = encryptionManager.encrypt(voteData)
                Log.d(TAG, "submitVote: encryption OK — generating integrity signature...")
                val integrity = IntegrityVerifier.generateVoteSignature(userId, electionId, candidateId, timestamp)
                hash = integrity.hash
                hmac = integrity.hmac  // S3: HMAC now persisted in Vote model
                verificationToken = integrity.verificationToken
                Log.d(TAG, "submitVote: integrity OK — token=${verificationToken.take(8)}...")
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
                verificationToken = verificationToken
            )

            Log.d(TAG, "submitVote: submitting to Firestore (transaction)...")
            when (val result = voteRepository.submitVote(vote)) {
                is Resource.Success -> {
                    Log.i(TAG, "submitVote: Firestore OK — incrementing counters...")
                    // Increment candidate vote count (fire-and-forget — non-critical)
                    try { candidateRepository.incrementVoteCount(candidateId) } catch (e: Exception) { Log.w(TAG, "incrementVoteCount failed (non-critical)", e) }
                    // Increment election voted count
                    try { electionRepository.incrementVotedCount(electionId) } catch (e: Exception) { Log.w(TAG, "incrementVotedCount failed (non-critical)", e) }
                    // Log audit
                    try {
                        auditRepository.log(
                            actorId = userId,
                            actorName = sessionManager.getUserName(),
                            actorRole = sessionManager.getUserRole()?.name ?: "",
                            action = AuditAction.VOTE_CAST,
                            target = electionId,
                            targetName = candidateId
                        )
                    } catch (e: Exception) { Log.w(TAG, "audit log failed (non-critical)", e) }
                    Log.i(TAG, "submitVote: SUCCESS! Vote cast securely.")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        verificationToken = verificationToken
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "submitVote: Firestore FAILED — ${result.message}")
                    _voteState.value = _voteState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetVoteState() {
        _voteState.value = VoteState()
    }

    fun clearVoteError() {
        _voteState.value = _voteState.value.copy(error = null)
    }
}
