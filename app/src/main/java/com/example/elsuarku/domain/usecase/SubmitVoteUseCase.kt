package com.example.elsuarku.domain.usecase

import android.util.Log
import com.example.elsuarku.data.model.AuditAction
import com.example.elsuarku.data.model.Election
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

/**
 * Orchestrates a secure vote submission with two-phase commit semantics.
 *
 * Phase 1 — Atomic write: Store the encrypted vote document in Firestore
 *           using a transaction with deterministic document ID (prevents double-voting).
 * Phase 2 — Counter reconciliation: Increment candidate + election vote counters.
 *           If counters fail, the vote is marked PENDING_RECONCILIATION for a
 *           background reconciliation job.
 *
 * This ensures the vote is never lost, even if counter increments fail due to
 * transient network issues or Firestore unavailability.
 */
class SubmitVoteUseCase(
    private val voteRepository: IVoteRepository,
    private val candidateRepository: ICandidateRepository,
    private val electionRepository: IElectionRepository,
    private val encryptionManager: EncryptionManager,
    private val sessionManager: SessionManager,
    private val auditRepository: IAuditRepository
) {
    companion object {
        private const val TAG = "SubmitVoteUseCase"
    }

    /**
     * Execute a secure vote submission.
     *
     * @param params The validated vote parameters
     * @param election The election being voted on (for validation)
     * @return SubmitVoteResult with verification token on success, error on failure
     */
    suspend fun execute(params: SubmitVoteParams, election: Election?): SubmitVoteResult {
        Log.i(TAG, "execute: starting — election=${params.electionId} candidate=${params.candidateId}")

        // Validate election state if provided
        election?.let { validateElection(it) }?.let { return it }

        // Compute anonymous voter hash
        val voterHash = IntegrityVerifier.computeVoterHash(params.userId, params.electionId)

        // Encrypt vote + HMAC integrity + verification token
        val (encryptedData, hash, hmac, verificationToken) = try {
            val timestamp = System.currentTimeMillis()
            val voteData = "vote:${params.userId}:${params.electionId}:${params.candidateId}:$timestamp"
            val encrypted = encryptionManager.encrypt(voteData)
            val integrity = IntegrityVerifier.generateVoteSignature(
                params.userId, params.electionId, params.candidateId, timestamp
            )
            Tuple4(encrypted, integrity.hash, integrity.hmac, integrity.verificationToken)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption/integrity failed", e)
            return SubmitVoteResult.Error(
                com.example.elsuarku.domain.error.AppError.Vote.EncryptionFailed(e)
            )
        }

        val vote = Vote(
            electionId = params.electionId,
            voterHash = voterHash,
            encryptedVoteData = encryptedData,
            hash = hash,
            hmac = hmac,
            verificationToken = verificationToken,
            reconciliationStatus = ReconciliationStatus.PENDING_RECONCILIATION
        )

        // Phase 1: Atomic write via Firestore transaction (prevents double-voting)
        Log.d(TAG, "PHASE 1: atomic vote write via transaction...")
        when (val result = voteRepository.submitVote(vote)) {
            is Resource.Success -> {
                Log.i(TAG, "PHASE 1 OK — vote persisted")

                // Phase 2a: Increment candidate counter
                val candidateOk = try {
                    candidateRepository.incrementVoteCount(params.candidateId)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "PHASE 2a FAILED — candidate counter", e)
                    false
                }

                // Phase 2b: Increment election counter
                val electionOk = try {
                    electionRepository.incrementVotedCount(params.electionId)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "PHASE 2b FAILED — election counter", e)
                    false
                }

                // Update reconciliation status if all counters synced
                if (candidateOk && electionOk) {
                    Log.i(TAG, "PHASE 2 OK — all counters synced")
                    try {
                        voteRepository.updateReconciliationStatus(
                            voterHash, params.electionId, ReconciliationStatus.CONFIRMED
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update reconciliation status (non-critical)", e)
                    }
                } else {
                    Log.w(TAG, "PHASE 2 PARTIAL — vote saved, counters need reconciliation")
                }

                // Phase 3: Audit log (non-critical, fire-and-forget)
                try {
                    auditRepository.log(
                        actorId = params.userId,
                        actorName = params.userName ?: "",
                        actorRole = params.userRole,
                        action = AuditAction.VOTE_CAST,
                        target = params.electionId,
                        targetName = params.candidateId,
                        detail = "reconciliation=${if (candidateOk && electionOk) "CONFIRMED" else "PENDING"}"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Audit log failed (non-critical)", e)
                }

                Log.i(TAG, "SUCCESS! Vote cast securely.")
                return SubmitVoteResult.Success(verificationToken)
            }
            is Resource.Error -> {
                Log.e(TAG, "PHASE 1 FAILED — ${result.message}")
                return SubmitVoteResult.Error(
                    com.example.elsuarku.domain.error.AppError.UnknownError(
                        RuntimeException(result.message)
                    )
                )
            }
            is Resource.Loading -> {}
            is Resource.Empty -> {}
            is Resource.Cached<*> -> {}
        }

        return SubmitVoteResult.Error(
            com.example.elsuarku.domain.error.AppError.UnknownError(
                RuntimeException("Unknown vote submission error")
            )
        )
    }

    private fun validateElection(election: Election): SubmitVoteResult.Error? {
        if (election.status != com.example.elsuarku.data.model.ElectionStatus.ACTIVE) {
            Log.w(TAG, "REJECTED — election status=${election.status}")
            return SubmitVoteResult.Error(
                com.example.elsuarku.domain.error.AppError.Vote.ElectionNotActive(
                    "Pemilihan sudah tidak aktif (status: ${election.status.name})"
                )
            )
        }
        if (System.currentTimeMillis() > election.endDate) {
            Log.w(TAG, "REJECTED — election expired")
            return SubmitVoteResult.Error(
                com.example.elsuarku.domain.error.AppError.Vote.ElectionExpired()
            )
        }
        if (System.currentTimeMillis() < election.startDate) {
            Log.w(TAG, "REJECTED — election not yet started")
            return SubmitVoteResult.Error(
                com.example.elsuarku.domain.error.AppError.Vote.ElectionNotStarted()
            )
        }
        return null
    }

    /** Internal 4-tuple for encryption result */
    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

/**
 * Parameters for vote submission.
 */
data class SubmitVoteParams(
    val userId: String,
    val userName: String?,
    val userRole: String,
    val electionId: String,
    val candidateId: String,
    val voterHash: String,
    val encryptedVoteData: String,
    val hash: String,
    val hmac: String,
    val verificationToken: String
)

/**
 * Result of a vote submission attempt.
 */
sealed class SubmitVoteResult {
    data class Success(val verificationToken: String) : SubmitVoteResult()
    data class Error(val error: com.example.elsuarku.domain.error.AppError) : SubmitVoteResult()
}
